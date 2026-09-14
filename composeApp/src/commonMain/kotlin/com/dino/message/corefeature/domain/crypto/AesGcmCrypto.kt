package com.dino.message.corefeature.domain.crypto

interface AesGcmCrypto {
    suspend fun generateKey(sizeBytes: Int): ByteArray
    suspend fun encrypt(plainText: ByteArray, key: ByteArray, ivSizeBytes: Int, tagLengthBits: Int): ByteArray
    suspend fun decrypt(encryptedPayload: ByteArray, key: ByteArray, ivSizeBytes: Int, tagLengthBits: Int): ByteArray
    suspend fun encryptWithIv(plainText: ByteArray, key: ByteArray, iv: ByteArray, tagLengthBits: Int): ByteArray
    suspend fun decryptLegacyEcb(encryptedPayload: ByteArray, key: ByteArray): ByteArray
}
