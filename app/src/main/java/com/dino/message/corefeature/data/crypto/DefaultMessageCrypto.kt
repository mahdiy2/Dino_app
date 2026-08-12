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
    override fun generateRsaKeyPair(): ExportedRsaKeyPair {
        val keyPair = rsaCrypto.generateKeyPair(RSA_KEY_SIZE_BITS)
        return ExportedRsaKeyPair(
            publicKeyBase64 = base64Codec.encodeNoWrap(keyPair.publicKey.encoded),
            privateKeyBase64 = base64Codec.encodeNoWrap(keyPair.privateKey.encoded),
        )
    }

    override fun decodePublicKey(rawValue: String): RsaPublicKey {
        return rsaCrypto.importPublicKey(rawValue)
    }

    override fun decodePublicKeyOrNull(rawValue: String?): RsaPublicKey? {
        return rsaCrypto.importPublicKeyOrNull(rawValue)
    }

    override fun decodePrivateKey(base64Key: String): RsaPrivateKey {
        return rsaCrypto.importPrivateKeyPkcs8(base64Codec.decodeDefault(base64Key))
    }

    override fun encryptMessage(
        message: String,
        receiverPublicKey: RsaPublicKey,
    ): EncryptedMessagePayload {
        val aesKey = aesGcmCrypto.generateKey(AES_KEY_SIZE_BYTES)
        val encryptedMessage = aesGcmCrypto.encrypt(
            plainText = message.toByteArray(Charsets.UTF_8),
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

    override fun encryptGroupMessage(
        message: String,
        memberPublicKeys: Map<String, RsaPublicKey>,
    ): GroupEncryptionPayload {
        val aesKey = aesGcmCrypto.generateKey(AES_KEY_SIZE_BYTES)
        val encryptedMessage = aesGcmCrypto.encrypt(
            plainText = message.toByteArray(Charsets.UTF_8),
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

    override fun encryptWithAesKey(message: String, aesKey: ByteArray): String {
        val encryptedMessage = aesGcmCrypto.encrypt(
            plainText = message.toByteArray(Charsets.UTF_8),
            key = aesKey,
            ivSizeBytes = GCM_IV_SIZE_BYTES,
            tagLengthBits = GCM_TAG_LENGTH_BITS,
        )
        return "$V2_PREFIX${base64Codec.encodeNoWrap(encryptedMessage)}"
    }

    override fun decryptMessage(
        encryptedMessage: String,
        encryptedAesKey: String,
        privateKey: RsaPrivateKey,
    ): String {
        val isMessageV2 = encryptedMessage.startsWith(V2_PREFIX)
        val isKeyV2 = encryptedAesKey.startsWith(V2_PREFIX)
        val normalizedMessage = encryptedMessage.removePrefix(V2_PREFIX)
        val normalizedAesKey = encryptedAesKey.removePrefix(V2_PREFIX)
        val attempts = if (isMessageV2 || isKeyV2) {
            listOf(::decryptV2, ::decryptLegacy)
        } else {
            listOf(::decryptLegacy, ::decryptV2)
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

    override fun formatPublicKeyForLog(rawKey: String?): String {
        if (rawKey == null) return "key=null"

        val trimmed = rawKey.trim()
        val escaped = trimmed
            .replace("\r", "\\r")
            .replace("\n", "\\n")
        val head = escaped.take(220)
        val tail = if (escaped.length > 300) escaped.takeLast(120) else ""
        val sha256 = runCatching {
            hashCrypto.sha256(trimmed.toByteArray())
                .joinToString("") { "%02x".format(it) }
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

    private fun decryptV2(
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
        return String(decryptedBytes, Charsets.UTF_8)
    }

    private fun decryptLegacy(
        encryptedMessage: String,
        encryptedAesKey: String,
        privateKey: RsaPrivateKey,
    ): String {
        val aesKey = rsaCrypto.decryptPkcs1(
            data = base64Codec.decodeNoWrap(encryptedAesKey),
            privateKey = privateKey,
        )
        val aesCipher = javax.crypto.Cipher.getInstance(AES_LEGACY_TRANSFORMATION)
        aesCipher.init(
            javax.crypto.Cipher.DECRYPT_MODE,
            javax.crypto.spec.SecretKeySpec(aesKey, AES_ALGORITHM),
        )
        val decryptedBytes = aesCipher.doFinal(base64Codec.decodeNoWrap(encryptedMessage))
        return String(decryptedBytes, Charsets.UTF_8)
    }

    private companion object {
        const val RSA_KEY_SIZE_BITS = 2048
        const val AES_KEY_SIZE_BYTES = 32
        const val GCM_IV_SIZE_BYTES = 12
        const val GCM_TAG_LENGTH_BITS = 128
        const val V2_PREFIX = "v2:"
        const val AES_ALGORITHM = "AES"
        const val AES_LEGACY_TRANSFORMATION = "AES"
    }
}
