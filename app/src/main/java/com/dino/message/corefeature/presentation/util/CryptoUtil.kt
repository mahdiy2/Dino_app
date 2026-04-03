package com.dino.message.corefeature.presentation.util

import android.util.Base64
import java.nio.charset.StandardCharsets
import java.security.PublicKey
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.OAEPParameterSpec
import javax.crypto.spec.PSource
import javax.crypto.spec.SecretKeySpec

object CryptoUtil {

    private const val AES_KEY_SIZE_BYTES = 32
    private const val GCM_IV_SIZE_BYTES = 12
    private const val GCM_TAG_LENGTH_BITS = 128
    private const val V2_PREFIX = "v2:"

    fun encryptMessage(message: String, receiverPublicKey: PublicKey): Pair<String, String> {
        val aesKey = ByteArray(AES_KEY_SIZE_BYTES)
        SecureRandom().nextBytes(aesKey)

        val iv = ByteArray(GCM_IV_SIZE_BYTES)
        SecureRandom().nextBytes(iv)

        val aesCipher = Cipher.getInstance("AES/GCM/NoPadding")
        val secretKey = SecretKeySpec(aesKey, "AES")
        val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv)
        aesCipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmSpec)
        val encryptedMsg = aesCipher.doFinal(message.toByteArray(StandardCharsets.UTF_8))

        val rsaCipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding")
        rsaCipher.init(
            Cipher.ENCRYPT_MODE,
            receiverPublicKey,
            OAEPParameterSpec("SHA-256", "MGF1", java.security.spec.MGF1ParameterSpec.SHA256, PSource.PSpecified.DEFAULT)
        )
        val encryptedAesKey = rsaCipher.doFinal(aesKey)

        val v2EncryptedMessage = Base64.encodeToString(iv + encryptedMsg, Base64.NO_WRAP)

        return Pair(
            "$V2_PREFIX${Base64.encodeToString(encryptedAesKey, Base64.NO_WRAP)}",
            "$V2_PREFIX$v2EncryptedMessage"
        )
    }

    data class GroupEncryptionResult(
        val encryptedMessage: String,
        val encryptedKeys: Map<String, String>,
        internal val rawAesKey: ByteArray,
    )

    fun encryptGroupMessage(
        message: String,
        memberPublicKeys: Map<String, PublicKey>,
    ): GroupEncryptionResult {
        val aesKey = ByteArray(AES_KEY_SIZE_BYTES)
        SecureRandom().nextBytes(aesKey)

        val iv = ByteArray(GCM_IV_SIZE_BYTES)
        SecureRandom().nextBytes(iv)

        val aesCipher = Cipher.getInstance("AES/GCM/NoPadding")
        val secretKey = SecretKeySpec(aesKey, "AES")
        val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv)
        aesCipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmSpec)
        val encryptedMsg = aesCipher.doFinal(message.toByteArray(StandardCharsets.UTF_8))

        val v2EncryptedMessage = "$V2_PREFIX${Base64.encodeToString(iv + encryptedMsg, Base64.NO_WRAP)}"

        val encryptedKeys = mutableMapOf<String, String>()
        for ((username, publicKey) in memberPublicKeys) {
            val rsaCipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding")
            rsaCipher.init(
                Cipher.ENCRYPT_MODE,
                publicKey,
                OAEPParameterSpec("SHA-256", "MGF1", java.security.spec.MGF1ParameterSpec.SHA256, PSource.PSpecified.DEFAULT)
            )
            val encryptedAesKey = rsaCipher.doFinal(aesKey)
            encryptedKeys[username] = "$V2_PREFIX${Base64.encodeToString(encryptedAesKey, Base64.NO_WRAP)}"
        }

        return GroupEncryptionResult(
            encryptedMessage = v2EncryptedMessage,
            encryptedKeys = encryptedKeys,
            rawAesKey = aesKey,
        )
    }

    fun encryptWithAesKey(
        message: String,
        aesKey: ByteArray,
    ): String {
        val iv = ByteArray(GCM_IV_SIZE_BYTES)
        SecureRandom().nextBytes(iv)

        val aesCipher = Cipher.getInstance("AES/GCM/NoPadding")
        val secretKey = SecretKeySpec(aesKey, "AES")
        val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv)
        aesCipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmSpec)
        val encryptedMsg = aesCipher.doFinal(message.toByteArray(StandardCharsets.UTF_8))

        return "$V2_PREFIX${Base64.encodeToString(iv + encryptedMsg, Base64.NO_WRAP)}"
    }
}
