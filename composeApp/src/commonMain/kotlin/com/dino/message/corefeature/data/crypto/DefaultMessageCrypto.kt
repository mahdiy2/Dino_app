package com.dino.message.corefeature.data.crypto

import com.dino.message.corefeature.domain.crypto.AesGcmCrypto
import com.dino.message.corefeature.domain.crypto.Base64Codec
import com.dino.message.corefeature.domain.crypto.EncryptedMessagePayload
import com.dino.message.corefeature.domain.crypto.ExportedRsaKeyPair
import com.dino.message.corefeature.domain.crypto.GroupEncryptionPayload
import com.dino.message.corefeature.domain.crypto.HashCrypto
import com.dino.message.corefeature.domain.crypto.MessageCrypto
import com.dino.message.corefeature.domain.crypto.RsaCrypto
import com.dino.message.corefeature.domain.crypto.RsaPrivateKey
import com.dino.message.corefeature.domain.crypto.RsaPublicKey

class DefaultMessageCrypto(
    private val base64Codec: Base64Codec,
    private val rsaCrypto: RsaCrypto,
    private val aesGcmCrypto: AesGcmCrypto,
    private val hashCrypto: HashCrypto,
) : MessageCrypto {
    override suspend fun generateRsaKeyPair(): ExportedRsaKeyPair {
        val keyPair = rsaCrypto.generateKeyPair(RSA_KEY_SIZE_BITS)
        return ExportedRsaKeyPair(
            publicKeyBase64 = base64Codec.encodeNoWrap(keyPair.publicKey.encoded),
            privateKeyBase64 = base64Codec.encodeNoWrap(keyPair.privateKey.encoded),
        )
    }

    override suspend fun decodePublicKey(rawValue: String): RsaPublicKey {
        return rsaCrypto.importPublicKey(rawValue)
    }

    override suspend fun decodePublicKeyOrNull(rawValue: String?): RsaPublicKey? {
        return rsaCrypto.importPublicKeyOrNull(rawValue)
    }

    override suspend fun decodePrivateKey(base64Key: String): RsaPrivateKey {
        return rsaCrypto.importPrivateKeyPkcs8(base64Codec.decodeDefault(base64Key))
    }

    override suspend fun encryptMessage(
        message: String,
        receiverPublicKey: RsaPublicKey,
    ): EncryptedMessagePayload {
        val aesKey = aesGcmCrypto.generateKey(AES_KEY_SIZE_BYTES)
        val encryptedMessage = aesGcmCrypto.encrypt(
            plainText = message.encodeToByteArray(),
            key = aesKey,
            ivSizeBytes = GCM_IV_SIZE_BYTES,
            tagLengthBits = GCM_TAG_LENGTH_BITS,
        )
        val encryptedAesKey = rsaCrypto.encryptOaepSha256(aesKey, receiverPublicKey)
        return EncryptedMessagePayload(
            encryptedKey = "$V2_PREFIX${base64Codec.encodeNoWrap(encryptedAesKey)}",
            encryptedMessage = "$V2_PREFIX${base64Codec.encodeNoWrap(encryptedMessage)}",
        )
    }

    override suspend fun encryptGroupMessage(
        message: String,
        memberPublicKeys: Map<String, RsaPublicKey>,
    ): GroupEncryptionPayload {
        val aesKey = aesGcmCrypto.generateKey(AES_KEY_SIZE_BYTES)
        val encryptedMessage = aesGcmCrypto.encrypt(
            plainText = message.encodeToByteArray(),
            key = aesKey,
            ivSizeBytes = GCM_IV_SIZE_BYTES,
            tagLengthBits = GCM_TAG_LENGTH_BITS,
        )
        val encryptedKeys = memberPublicKeys.mapValues { (_, publicKey) ->
            "$V2_PREFIX${base64Codec.encodeNoWrap(rsaCrypto.encryptOaepSha256(aesKey, publicKey))}"
        }
        return GroupEncryptionPayload(
            encryptedMessage = "$V2_PREFIX${base64Codec.encodeNoWrap(encryptedMessage)}",
            encryptedKeys = encryptedKeys,
            rawAesKey = aesKey,
        )
    }

    override suspend fun encryptWithAesKey(message: String, aesKey: ByteArray): String {
        val encryptedMessage = aesGcmCrypto.encrypt(
            plainText = message.encodeToByteArray(),
            key = aesKey,
            ivSizeBytes = GCM_IV_SIZE_BYTES,
            tagLengthBits = GCM_TAG_LENGTH_BITS,
        )
        return "$V2_PREFIX${base64Codec.encodeNoWrap(encryptedMessage)}"
    }

    override suspend fun decryptMessage(
        encryptedMessage: String,
        encryptedAesKey: String,
        privateKey: RsaPrivateKey,
    ): String {
        val isMessageV2 = encryptedMessage.startsWith(V2_PREFIX)
        val isKeyV2 = encryptedAesKey.startsWith(V2_PREFIX)
        val normalizedMessage = encryptedMessage.removePrefix(V2_PREFIX)
        val normalizedAesKey = encryptedAesKey.removePrefix(V2_PREFIX)
        val attempts = if (isMessageV2 || isKeyV2) {
            listOf<suspend (String, String, RsaPrivateKey) -> String>(::decryptV2, ::decryptLegacy)
        } else {
            listOf<suspend (String, String, RsaPrivateKey) -> String>(::decryptLegacy, ::decryptV2)
        }

        var lastError: Exception? = null
        for (attempt in attempts) {
            try {
                return attempt(normalizedMessage, normalizedAesKey, privateKey)
            } catch (e: Exception) {
                lastError = e
            }
        }
        throw lastError ?: IllegalStateException("Failed to decrypt message")
    }

    override suspend fun formatPublicKeyForLog(rawKey: String?): String {
        if (rawKey == null) return "key=null"

        val trimmed = rawKey.trim()
        val escaped = trimmed
            .replace("\r", "\\r")
            .replace("\n", "\\n")
        val head = escaped.take(220)
        val tail = if (escaped.length > 300) escaped.takeLast(120) else ""
        val sha256 = runCatching {
            hashCrypto.sha256(trimmed.encodeToByteArray())
                .joinToString("") { byte ->
                    val value = byte.toInt() and 0xff
                    HEX[value ushr 4].toString() + HEX[value and 0x0f]
                }
                .take(16)
        }.getOrElse { "n/a" }

        return buildString {
            append("len=").append(rawKey.length)
            append(", trimmedLen=").append(trimmed.length)
            append(", sha256=").append(sha256)
            append(", keyHead=").append(head)
            if (tail.isNotEmpty()) {
                append(", keyTail=").append(tail)
            }
        }
    }

    private suspend fun decryptV2(
        encryptedMessage: String,
        encryptedAesKey: String,
        privateKey: RsaPrivateKey,
    ): String {
        val aesKey = rsaCrypto.decryptOaepSha256(
            data = base64Codec.decodeNoWrap(encryptedAesKey),
            privateKey = privateKey,
        )
        val decryptedBytes = aesGcmCrypto.decrypt(
            encryptedPayload = base64Codec.decodeNoWrap(encryptedMessage),
            key = aesKey,
            ivSizeBytes = GCM_IV_SIZE_BYTES,
            tagLengthBits = GCM_TAG_LENGTH_BITS,
        )
        return decryptedBytes.decodeToString()
    }

    private suspend fun decryptLegacy(
        encryptedMessage: String,
        encryptedAesKey: String,
        privateKey: RsaPrivateKey,
    ): String {
        val aesKey = rsaCrypto.decryptPkcs1(
            data = base64Codec.decodeNoWrap(encryptedAesKey),
            privateKey = privateKey,
        )
        val decryptedBytes = aesGcmCrypto.decryptLegacyEcb(
            encryptedPayload = base64Codec.decodeNoWrap(encryptedMessage),
            key = aesKey,
        )
        return decryptedBytes.decodeToString()
    }

    private companion object {
        const val RSA_KEY_SIZE_BITS = 2048
        const val AES_KEY_SIZE_BYTES = 32
        const val GCM_IV_SIZE_BYTES = 12
        const val GCM_TAG_LENGTH_BITS = 128
        const val V2_PREFIX = "v2:"
        const val HEX = "0123456789abcdef"
    }
}
