package com.dino.message.corefeature.data.crypto

import com.dino.message.corefeature.domain.crypto.AesGcmCrypto
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

class JvmAesGcmCrypto(
    private val secureRandom: SecureRandom = SecureRandom(),
) : AesGcmCrypto {
    override suspend fun generateKey(sizeBytes: Int): ByteArray {
        return ByteArray(sizeBytes).also(secureRandom::nextBytes)
    }

    override suspend fun encrypt(
        plainText: ByteArray,
        key: ByteArray,
        ivSizeBytes: Int,
        tagLengthBits: Int,
    ): ByteArray {
        val iv = ByteArray(ivSizeBytes).also(secureRandom::nextBytes)
        return encryptWithIv(plainText, key, iv, tagLengthBits)
    }

    override suspend fun decrypt(
        encryptedPayload: ByteArray,
        key: ByteArray,
        ivSizeBytes: Int,
        tagLengthBits: Int,
    ): ByteArray {
        require(encryptedPayload.size > ivSizeBytes) { "Invalid encrypted payload" }
        val iv = encryptedPayload.copyOfRange(0, ivSizeBytes)
        val cipherText = encryptedPayload.copyOfRange(ivSizeBytes, encryptedPayload.size)
        val aesCipher = Cipher.getInstance(AES_GCM_TRANSFORMATION)
        aesCipher.init(
            Cipher.DECRYPT_MODE,
            SecretKeySpec(key, AES_ALGORITHM),
            GCMParameterSpec(tagLengthBits, iv),
        )
        return aesCipher.doFinal(cipherText)
    }

    override suspend fun encryptWithIv(
        plainText: ByteArray,
        key: ByteArray,
        iv: ByteArray,
        tagLengthBits: Int,
    ): ByteArray {
        val aesCipher = Cipher.getInstance(AES_GCM_TRANSFORMATION)
        aesCipher.init(
            Cipher.ENCRYPT_MODE,
            SecretKeySpec(key, AES_ALGORITHM),
            GCMParameterSpec(tagLengthBits, iv),
        )
        return iv + aesCipher.doFinal(plainText)
    }

    override suspend fun decryptLegacyEcb(encryptedPayload: ByteArray, key: ByteArray): ByteArray {
        val aesCipher = Cipher.getInstance(AES_ALGORITHM)
        aesCipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, AES_ALGORITHM))
        return aesCipher.doFinal(encryptedPayload)
    }

    private companion object {
        const val AES_ALGORITHM = "AES"
        const val AES_GCM_TRANSFORMATION = "AES/GCM/NoPadding"
    }
}
