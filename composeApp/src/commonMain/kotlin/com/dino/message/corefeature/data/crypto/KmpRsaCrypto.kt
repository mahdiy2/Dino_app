package com.dino.message.corefeature.data.crypto

import com.dino.message.corefeature.domain.crypto.Base64Codec
import com.dino.message.corefeature.domain.crypto.RsaCrypto
import com.dino.message.corefeature.domain.crypto.RsaKeyPair
import com.dino.message.corefeature.domain.crypto.RsaPrivateKey
import com.dino.message.corefeature.domain.crypto.RsaPublicKey
import dev.whyoleg.cryptography.BinarySize.Companion.bits
import dev.whyoleg.cryptography.CryptographyProvider
import dev.whyoleg.cryptography.DelicateCryptographyApi
import dev.whyoleg.cryptography.algorithms.RSA
import dev.whyoleg.cryptography.algorithms.SHA256

private data class EncodedRsaPublicKey(override val encoded: ByteArray) : RsaPublicKey
private data class EncodedRsaPrivateKey(override val encoded: ByteArray) : RsaPrivateKey

@OptIn(DelicateCryptographyApi::class)
class KmpRsaCrypto(
    private val base64Codec: Base64Codec,
    private val provider: CryptographyProvider = CryptographyProvider.Default,
) : RsaCrypto {
    private val oaep: RSA.OAEP
        get() = provider.get(RSA.OAEP)

    override suspend fun generateKeyPair(keySizeBits: Int): RsaKeyPair {
        val pair = oaep.keyPairGenerator(keySizeBits.bits, SHA256).generateKey()
        return RsaKeyPair(
            publicKey = EncodedRsaPublicKey(pair.publicKey.encodeToByteArray(RSA.PublicKey.Format.DER)),
            privateKey = EncodedRsaPrivateKey(pair.privateKey.encodeToByteArray(RSA.PrivateKey.Format.DER)),
        )
    }

    override suspend fun importPrivateKeyPkcs8(keyBytes: ByteArray): RsaPrivateKey {
        oaep.privateKeyDecoder(SHA256).decodeFromByteArray(RSA.PrivateKey.Format.DER, keyBytes)
        return EncodedRsaPrivateKey(keyBytes)
    }

    override suspend fun importPublicKey(rawValue: String): RsaPublicKey =
        importPublicKeyInternal(rawValue, depth = 0)

    override suspend fun importPublicKeyOrNull(rawValue: String?): RsaPublicKey? {
        if (rawValue.isNullOrBlank()) return null
        return runCatching { importPublicKey(rawValue) }.getOrNull()
    }

    override suspend fun encryptOaepSha256(data: ByteArray, publicKey: RsaPublicKey): ByteArray {
        val key = oaep.publicKeyDecoder(SHA256)
            .decodeFromByteArray(RSA.PublicKey.Format.DER, publicKey.encoded)
        return key.encryptor().encrypt(data)
    }

    override suspend fun decryptOaepSha256(data: ByteArray, privateKey: RsaPrivateKey): ByteArray {
        val key = oaep.privateKeyDecoder(SHA256)
            .decodeFromByteArray(RSA.PrivateKey.Format.DER, privateKey.encoded)
        return key.decryptor().decrypt(data)
    }

    override suspend fun decryptPkcs1(data: ByteArray, privateKey: RsaPrivateKey): ByteArray {
        if (requiresPlatformLegacyRsaDecryptor) {
            return decryptLegacyRsaPkcs1Platform(privateKey.encoded, data)
        }
        val algorithm = provider.getOrNull(RSA.PKCS1)
            ?: return decryptLegacyRsaPkcs1Platform(privateKey.encoded, data)
        val key = algorithm.privateKeyDecoder(SHA256)
            .decodeFromByteArray(RSA.PrivateKey.Format.DER, privateKey.encoded)
        return key.decryptor().decrypt(data)
    }

    private suspend fun importPublicKeyInternal(rawValue: String, depth: Int): RsaPublicKey {
        require(depth <= MAX_NESTING_DEPTH) { "Unsupported public key format" }
        val sanitized = rawValue.trim()
            .trim('"')
            .replace("\\\\n", "\n")
            .replace("\\n", "\n")
            .replace("\\r", "")
            .substringAfter("base64,")

        parseOpenSshKey(sanitized)?.let { return importPublicDer(it, RSA.PublicKey.Format.DER.PKCS1) }

        val isPkcs1 = sanitized.contains("BEGIN RSA PUBLIC KEY")
        val normalized = sanitized
            .replace("-----BEGIN PUBLIC KEY-----", "")
            .replace("-----END PUBLIC KEY-----", "")
            .replace("-----BEGIN RSA PUBLIC KEY-----", "")
            .replace("-----END RSA PUBLIC KEY-----", "")
            .replace("-----BEGIN CERTIFICATE-----", "")
            .replace("-----END CERTIFICATE-----", "")
            .filterNot(Char::isWhitespace)

        val decodedCandidates = listOfNotNull(
            runCatching { base64Codec.decodeDefault(normalized) }.getOrNull(),
            runCatching { base64Codec.decodeNoWrap(normalized) }.getOrNull(),
            runCatching { base64Codec.decodeUrlSafe(normalized, noWrap = false) }.getOrNull(),
            runCatching { base64Codec.decodeUrlSafe(normalized, noWrap = true) }.getOrNull(),
        ).distinctBy(ByteArray::contentHashCode)

        for (candidate in decodedCandidates) {
            val formats = if (isPkcs1) {
                listOf(RSA.PublicKey.Format.DER.PKCS1, RSA.PublicKey.Format.DER)
            } else {
                listOf(RSA.PublicKey.Format.DER, RSA.PublicKey.Format.DER.PKCS1)
            }
            for (format in formats) {
                runCatching { importPublicDer(candidate, format) }.getOrNull()?.let { return it }
            }

            extractRsaSubjectPublicKeyInfo(candidate)?.let { subjectPublicKeyInfo ->
                runCatching {
                    importPublicDer(subjectPublicKeyInfo, RSA.PublicKey.Format.DER)
                }.getOrNull()?.let { return it }
            }

            val nested = candidate.decodeToString()
            if (nested.contains("BEGIN PUBLIC KEY") || nested.contains("BEGIN RSA PUBLIC KEY")) {
                return importPublicKeyInternal(nested, depth + 1)
            }
        }
        throw IllegalArgumentException("Unsupported public key format")
    }

    private suspend fun importPublicDer(
        bytes: ByteArray,
        format: RSA.PublicKey.Format,
    ): RsaPublicKey {
        val decoded = oaep.publicKeyDecoder(SHA256).decodeFromByteArray(format, bytes)
        return EncodedRsaPublicKey(decoded.encodeToByteArray(RSA.PublicKey.Format.DER))
    }

    private fun parseOpenSshKey(rawValue: String): ByteArray? {
        val parts = rawValue.trim().split(Regex("\\s+"), limit = 3)
        if (parts.size < 2 || parts[0] != "ssh-rsa") return null
        val decoded = runCatching { base64Codec.decodeDefault(parts[1]) }.getOrNull() ?: return null
        var index = 0

        fun readBytes(): ByteArray {
            require(index + 4 <= decoded.size) { "Invalid OpenSSH key" }
            val length = ((decoded[index].toInt() and 0xff) shl 24) or
                ((decoded[index + 1].toInt() and 0xff) shl 16) or
                ((decoded[index + 2].toInt() and 0xff) shl 8) or
                (decoded[index + 3].toInt() and 0xff)
            index += 4
            require(length >= 0 && index + length <= decoded.size) { "Invalid OpenSSH key" }
            return decoded.copyOfRange(index, index + length).also { index += length }
        }

        return runCatching {
            require(readBytes().decodeToString() == "ssh-rsa")
            val exponent = readBytes()
            val modulus = readBytes()
            derSequence(derInteger(modulus), derInteger(exponent))
        }.getOrNull()
    }

    private fun derSequence(vararg values: ByteArray): ByteArray {
        val content = values.fold(ByteArray(0), ByteArray::plus)
        return byteArrayOf(DER_SEQUENCE) + derLength(content.size) + content
    }

    private fun derInteger(raw: ByteArray): ByteArray {
        var value = raw.dropWhile { it == 0.toByte() }.toByteArray()
        if (value.isEmpty()) value = byteArrayOf(0)
        if (value[0].toInt() and 0x80 != 0) value = byteArrayOf(0) + value
        return byteArrayOf(DER_INTEGER) + derLength(value.size) + value
    }

    private fun derLength(length: Int): ByteArray {
        if (length < 0x80) return byteArrayOf(length.toByte())
        val bytes = buildList {
            var remaining = length
            while (remaining > 0) {
                add(0, (remaining and 0xff).toByte())
                remaining = remaining ushr 8
            }
        }.toByteArray()
        return byteArrayOf((0x80 or bytes.size).toByte()) + bytes
    }

    /** Extracts an RSA SubjectPublicKeyInfo sequence from an X.509 certificate. */
    private fun extractRsaSubjectPublicKeyInfo(der: ByteArray): ByteArray? {
        fun readTlv(index: Int, limit: Int): DerTlv? {
            if (index + 2 > limit) return null
            val tag = der[index].toInt() and 0xff
            var cursor = index + 1
            val firstLength = der[cursor++].toInt() and 0xff
            val length = if (firstLength and 0x80 == 0) {
                firstLength
            } else {
                val byteCount = firstLength and 0x7f
                if (byteCount == 0 || byteCount > 4 || cursor + byteCount > limit) return null
                var parsed = 0
                repeat(byteCount) { parsed = (parsed shl 8) or (der[cursor++].toInt() and 0xff) }
                parsed
            }
            val end = cursor + length
            if (end > limit) return null
            return DerTlv(tag = tag, start = index, contentStart = cursor, end = end)
        }

        fun containsRsaEncryptionOid(tlv: DerTlv): Boolean {
            if (tlv.tag != DER_SEQUENCE.toInt()) return false
            val content = der.copyOfRange(tlv.contentStart, tlv.end)
            return content.indexOfSubArray(RSA_ENCRYPTION_OID) >= 0
        }

        fun search(start: Int, end: Int, depth: Int): ByteArray? {
            if (depth > MAX_DER_SEARCH_DEPTH) return null
            var cursor = start
            while (cursor < end) {
                val tlv = readTlv(cursor, end) ?: return null
                if (tlv.tag == DER_SEQUENCE.toInt()) {
                    val algorithm = readTlv(tlv.contentStart, tlv.end)
                    val publicKeyBits = algorithm?.let { readTlv(it.end, tlv.end) }
                    if (algorithm != null && publicKeyBits?.tag == DER_BIT_STRING && containsRsaEncryptionOid(algorithm)) {
                        return der.copyOfRange(tlv.start, tlv.end)
                    }
                    search(tlv.contentStart, tlv.end, depth + 1)?.let { return it }
                }
                cursor = tlv.end
            }
            return null
        }

        return search(0, der.size, 0)
    }

    private fun ByteArray.indexOfSubArray(needle: ByteArray): Int {
        if (needle.isEmpty() || needle.size > size) return -1
        for (start in 0..size - needle.size) {
            var matches = true
            for (offset in needle.indices) {
                if (this[start + offset] != needle[offset]) {
                    matches = false
                    break
                }
            }
            if (matches) return start
        }
        return -1
    }

    private data class DerTlv(
        val tag: Int,
        val start: Int,
        val contentStart: Int,
        val end: Int,
    )

    private companion object {
        const val MAX_NESTING_DEPTH = 2
        const val MAX_DER_SEARCH_DEPTH = 8
        const val DER_SEQUENCE: Byte = 0x30
        const val DER_INTEGER: Byte = 0x02
        const val DER_BIT_STRING = 0x03
        val RSA_ENCRYPTION_OID = byteArrayOf(
            0x06, 0x09, 0x2a, 0x86.toByte(), 0x48, 0x86.toByte(), 0xf7.toByte(),
            0x0d, 0x01, 0x01, 0x01,
        )
    }
}
