package com.dino.message.corefeature.domain.crypto

data class EncryptedMessagePayload(
    val encryptedKey: String,
    val encryptedMessage: String,
)

data class GroupEncryptionPayload(
    val encryptedMessage: String,
    val encryptedKeys: Map<String, String>,
    val rawAesKey: ByteArray,
)

data class ExportedRsaKeyPair(
    val publicKeyBase64: String,
    val privateKeyBase64: String,
)

interface MessageCrypto {
    fun generateRsaKeyPair(): ExportedRsaKeyPair
    fun decodePublicKey(rawValue: String): RsaPublicKey
    fun decodePublicKeyOrNull(rawValue: String?): RsaPublicKey?
    fun decodePrivateKey(base64Key: String): RsaPrivateKey
    fun encryptMessage(message: String, receiverPublicKey: RsaPublicKey): EncryptedMessagePayload
    fun encryptGroupMessage(
        message: String,
        memberPublicKeys: Map<String, RsaPublicKey>,
    ): GroupEncryptionPayload
    fun encryptWithAesKey(message: String, aesKey: ByteArray): String
    fun decryptMessage(
        encryptedMessage: String,
        encryptedAesKey: String,
        privateKey: RsaPrivateKey,
    ): String
    fun formatPublicKeyForLog(rawKey: String?): String
}
