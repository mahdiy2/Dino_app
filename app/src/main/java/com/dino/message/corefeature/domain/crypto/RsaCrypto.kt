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
    fun generateKeyPair(keySizeBits: Int): RsaKeyPair
    fun importPrivateKeyPkcs8(keyBytes: ByteArray): RsaPrivateKey
    fun importPublicKey(rawValue: String): RsaPublicKey
    fun importPublicKeyOrNull(rawValue: String?): RsaPublicKey?
    fun encryptOaepSha256(data: ByteArray, publicKey: RsaPublicKey): ByteArray
    fun decryptOaepSha256(data: ByteArray, privateKey: RsaPrivateKey): ByteArray
    fun decryptPkcs1(data: ByteArray, privateKey: RsaPrivateKey): ByteArray
}
