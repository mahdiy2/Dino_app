package com.dino.message.corefeature.domain.crypto

interface RsaPublicKey {
    val encoded: ByteArray
}

interface RsaPrivateKey {
    val encoded: ByteArray
}

data class RsaKeyPair(
    val publicKey: RsaPublicKey,
    val privateKey: RsaPrivateKey,
)

interface RsaCrypto {
    suspend fun generateKeyPair(keySizeBits: Int): RsaKeyPair
    suspend fun importPrivateKeyPkcs8(keyBytes: ByteArray): RsaPrivateKey
    suspend fun importPublicKey(rawValue: String): RsaPublicKey
    suspend fun importPublicKeyOrNull(rawValue: String?): RsaPublicKey?
    suspend fun encryptOaepSha256(data: ByteArray, publicKey: RsaPublicKey): ByteArray
    suspend fun decryptOaepSha256(data: ByteArray, privateKey: RsaPrivateKey): ByteArray
    suspend fun decryptPkcs1(data: ByteArray, privateKey: RsaPrivateKey): ByteArray
}
