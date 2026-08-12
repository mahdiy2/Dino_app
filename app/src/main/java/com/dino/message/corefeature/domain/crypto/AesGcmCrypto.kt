package com.dino.message.corefeature.domain.crypto

interface AesGcmCrypto {
    fun generateKey(sizeBytes: Int): ByteArray
    fun encrypt(plainText: ByteArray, key: ByteArray, ivSizeBytes: Int, tagLengthBits: Int): ByteArray
    fun decrypt(encryptedPayload: ByteArray, key: ByteArray, ivSizeBytes: Int, tagLengthBits: Int): ByteArray
    fun encryptWithIv(plainText: ByteArray, key: ByteArray, iv: ByteArray, tagLengthBits: Int): ByteArray
}
