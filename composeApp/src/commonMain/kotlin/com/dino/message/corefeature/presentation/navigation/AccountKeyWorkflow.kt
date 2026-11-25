package com.dino.message.corefeature.presentation.navigation

import com.dino.message.authfeature.data.remote.dto.RegisterStartRequest
import com.dino.message.authfeature.domain.model.AccountKeyRecord
import com.dino.message.authfeature.domain.model.AccountKeyVault
import com.dino.message.corefeature.domain.crypto.MessageCrypto
import com.dino.message.corefeature.domain.crypto.RsaPrivateKey
import com.dino.message.corefeature.data.remote.util.CommonUuid

internal class AccountKeyWorkflow(
    private val vault: AccountKeyVault,
    private val api: AccountKeyApi,
    private val registration: ShellRegistrationApi,
    private val crypto: MessageCrypto,
    val needsPassphrase: Boolean,
) {
    suspend fun open(passphrase: String): AccountKeyRecord? {
        val identity = api.currentKey()
        var unlockFailure: IllegalStateException? = null
        for (slot in listOf(rotationSlot(identity.username), registrationSlot(identity.username), identity.username)) {
            val saved = try {
                vault.read(slot, passphrase)
            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                throw cancelled
            } catch (error: IllegalStateException) {
                unlockFailure = error
                continue
            } ?: continue
            if (matches(identity.publicKey, saved.privateKey)) {
                if (slot != identity.username) promote(identity.username, passphrase, slot, saved)
                return saved.copy(pendingRegistrationId = null, registrationNonce = null)
            }
        }
        unlockFailure?.let { throw it }
        return null
    }

    suspend fun importBackup(passphrase: String, privateKey: String): AccountKeyRecord {
        val identity = api.currentKey()
        check(matches(identity.publicKey, privateKey)) { "This backup does not match the account key." }
        val record = AccountKeyRecord(privateKey, checkNotNull(identity.publicKey))
        vault.write(identity.username, passphrase, record)
        return record
    }

    suspend fun prepareReset(passphrase: String): AccountKeyRecord {
        val slot = rotationSlot(api.currentKey().username)
        return vault.read(slot, passphrase) ?: generate().also { vault.write(slot, passphrase, it) }
    }

    suspend fun replaceKey(passphrase: String, password: String): AccountKeyRecord {
        val identity = api.currentKey()
        val slot = rotationSlot(identity.username)
        val staged = checkNotNull(vault.read(slot, passphrase))
        if (!matches(identity.publicKey, staged.privateKey)) {
            api.rotate(password, staged.publicKey)
            check(matches(api.currentKey().publicKey, staged.privateKey)) { "The account key has not changed yet." }
        }
        return promote(identity.username, passphrase, slot, staged)
    }

    suspend fun beginRegistration(username: String, password: String, passphrase: String, referralCode: String?): AccountKeyRecord {
        val slot = registrationSlot(username)
        val staged = vault.read(slot, passphrase) ?: generate().copy(
            registrationNonce = CommonUuid.randomUuidString(),
        ).also { vault.write(slot, passphrase, it) }
        if (staged.pendingRegistrationId != null) return staged
        val id = registration.start(RegisterStartRequest(username, password, staged.publicKey, checkNotNull(staged.registrationNonce), referral_code = referralCode))
        return staged.copy(pendingRegistrationId = id).also { vault.write(slot, passphrase, it) }
    }

    suspend fun confirmRegistration(username: String, passphrase: String): Int {
        val record = checkNotNull(vault.read(registrationSlot(username), passphrase))
        val status = registration.confirm(checkNotNull(record.pendingRegistrationId))
        if (status == 410) {
            vault.remove(registrationSlot(username))
        }
        return status
    }

    private suspend fun promote(
        account: String, passphrase: String, slot: String, record: AccountKeyRecord,
    ): AccountKeyRecord = record.copy(pendingRegistrationId = null, registrationNonce = null).also {
        vault.write(account, passphrase, it)
        vault.remove(slot)
    }

    private suspend fun generate() = crypto.generateRsaKeyPair().let {
        AccountKeyRecord(it.privateKeyBase64, it.publicKeyBase64)
    }

    private suspend fun matches(publicKey: String?, privateKey: String): Boolean =
        publicKey != null && verifiedBackup(crypto, publicKey, privateKey) != null

    private fun rotationSlot(account: String) = "rotation:$account"
    private fun registrationSlot(account: String) = "registration:$account"
}

internal suspend fun verifiedBackup(
    crypto: MessageCrypto,
    publicKey: String,
    privateKey: String,
): RsaPrivateKey? {
    return try {
        val decoded = crypto.decodePrivateKey(privateKey)
        val challenge = CommonUuid.randomUuidString()
        val encrypted = crypto.encryptMessage(challenge, crypto.decodePublicKey(publicKey))
        if (crypto.decryptMessage(encrypted.encryptedMessage, encrypted.encryptedKey, decoded) == challenge) decoded else null
    } catch (cancelled: kotlinx.coroutines.CancellationException) {
        throw cancelled
    } catch (_: Throwable) {
        null
    }
}
