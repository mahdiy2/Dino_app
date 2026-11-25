package com.dino.message.authfeature.domain.model

import kotlinx.serialization.Serializable

@Serializable
internal data class AccountKeyRecord(
    val privateKey: String,
    val publicKey: String,
    val pendingRegistrationId: String? = null,
    val registrationNonce: String? = null,
)

internal interface AccountKeyVault {
    suspend fun exists(account: String): Boolean
    suspend fun read(account: String, passphrase: String): AccountKeyRecord?
    suspend fun remove(account: String)
    suspend fun write(account: String, passphrase: String, record: AccountKeyRecord)
}
