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
    suspend fun generateRsaKeyPair(): ExportedRsaKeyPair
    suspend fun decodePublicKey(rawValue: String): RsaPublicKey
    suspend fun decodePublicKeyOrNull(rawValue: String?): RsaPublicKey?
    suspend fun decodePrivateKey(base64Key: String): RsaPrivateKey
    suspend fun encryptMessage(message: String, receiverPublicKey: RsaPublicKey): EncryptedMessagePayload
    suspend fun encryptGroupMessage(
        message: String,
        memberPublicKeys: Map<String, RsaPublicKey>,
    ): GroupEncryptionPayload
    suspend fun encryptWithAesKey(message: String, aesKey: ByteArray): String
    suspend fun decryptMessage(
        encryptedMessage: String,
        encryptedAesKey: String,
        privateKey: RsaPrivateKey,
    ): String
}
