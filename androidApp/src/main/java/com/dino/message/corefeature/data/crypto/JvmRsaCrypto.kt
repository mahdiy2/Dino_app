package com.dino.message.corefeature.data.crypto

import com.dino.message.corefeature.domain.crypto.Base64Codec
import com.dino.message.corefeature.domain.crypto.RsaCrypto
import com.dino.message.corefeature.domain.crypto.RsaKeyPair
import com.dino.message.corefeature.domain.crypto.RsaPrivateKey
import com.dino.message.corefeature.domain.crypto.RsaPublicKey
import java.io.ByteArrayInputStream
import java.math.BigInteger
import java.security.KeyFactory
import java.security.KeyPairGenerator
import java.security.PrivateKey
import java.security.PublicKey
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.security.spec.MGF1ParameterSpec
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.RSAPublicKeySpec
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Cipher
import javax.crypto.spec.OAEPParameterSpec
import javax.crypto.spec.PSource

class JvmRsaPublicKey(
    val delegate: PublicKey,
) : RsaPublicKey {
    override val encoded: ByteArray
        get() = delegate.encoded
}

class JvmRsaPrivateKey(
    val delegate: PrivateKey,
) : RsaPrivateKey {
    override val encoded: ByteArray
        get() = delegate.encoded
}

class JvmRsaCrypto(
    private val base64Codec: Base64Codec,
) : RsaCrypto {
    private val keyFactory: KeyFactory
        get() = KeyFactory.getInstance(RSA_ALGORITHM)

    override fun generateKeyPair(keySizeBits: Int): RsaKeyPair {
        val keyGen = KeyPairGenerator.getInstance(RSA_ALGORITHM)
        keyGen.initialize(keySizeBits)
        val keyPair = keyGen.generateKeyPair()
        return RsaKeyPair(
            publicKey = JvmRsaPublicKey(keyPair.public),
            privateKey = JvmRsaPrivateKey(keyPair.private),
        )
    }

    override fun importPrivateKeyPkcs8(keyBytes: ByteArray): RsaPrivateKey {
        return JvmRsaPrivateKey(keyFactory.generatePrivate(PKCS8EncodedKeySpec(keyBytes)))
    }

    override fun importPublicKey(rawValue: String): RsaPublicKey {
        return importPublicKeyInternal(rawValue, depth = 0)
    }

    override fun importPublicKeyOrNull(rawValue: String?): RsaPublicKey? {
        if (rawValue.isNullOrBlank()) return null
        return try {
            importPublicKey(rawValue)
        } catch (_: Exception) {
            null
        }
    }

    override fun encryptOaepSha256(data: ByteArray, publicKey: RsaPublicKey): ByteArray {
        val rsaCipher = Cipher.getInstance(RSA_OAEP_SHA_256_TRANSFORMATION)
        rsaCipher.init(Cipher.ENCRYPT_MODE, publicKey.asJvmKey(), oaepSha256Spec())
        return rsaCipher.doFinal(data)
    }

    override fun decryptOaepSha256(data: ByteArray, privateKey: RsaPrivateKey): ByteArray {
        val rsaCipher = Cipher.getInstance(RSA_OAEP_SHA_256_TRANSFORMATION)
        rsaCipher.init(Cipher.DECRYPT_MODE, privateKey.asJvmKey(), oaepSha256Spec())
        return rsaCipher.doFinal(data)
    }

    override fun decryptPkcs1(data: ByteArray, privateKey: RsaPrivateKey): ByteArray {
        val rsaCipher = Cipher.getInstance(RSA_PKCS1_TRANSFORMATION)
        rsaCipher.init(Cipher.DECRYPT_MODE, privateKey.asJvmKey())
        return rsaCipher.doFinal(data)
    }

    private fun importPublicKeyInternal(rawValue: String, depth: Int): RsaPublicKey {
        require(depth <= 2) { "Unsupported public key format" }

        val sanitized = rawValue
            .trim()
            .trim('"')
            .replace("\\\\n", "\n")
            .replace("\\n", "\n")
            .replace("\\r", "")
            .substringAfter("base64,")

        parseOpenSshKey(sanitized)?.let { return it }

        val normalizedKey = sanitized
            .replace("-----BEGIN PUBLIC KEY-----", "")
            .replace("-----END PUBLIC KEY-----", "")
            .replace("-----BEGIN RSA PUBLIC KEY-----", "")
            .replace("-----END RSA PUBLIC KEY-----", "")
            .replace("-----BEGIN CERTIFICATE-----", "")
            .replace("-----END CERTIFICATE-----", "")
            .replace("\\s".toRegex(), "")

        val padded = when (normalizedKey.length % 4) {
            2 -> "$normalizedKey=="
            3 -> "$normalizedKey="
            else -> normalizedKey
        }

        val decodedCandidates = listOfNotNull(
            runCatching { base64Codec.decodeDefault(padded) }.getOrNull(),
            runCatching { base64Codec.decodeNoWrap(padded) }.getOrNull(),
            runCatching { base64Codec.decodeUrlSafe(padded, noWrap = false) }.getOrNull(),
            runCatching { base64Codec.decodeUrlSafe(padded, noWrap = true) }.getOrNull(),
        ).distinctBy { it.contentHashCode() }

        val certFactory = CertificateFactory.getInstance("X.509")
        for (keyBytes in decodedCandidates) {
            try {
                return JvmRsaPublicKey(keyFactory.generatePublic(X509EncodedKeySpec(keyBytes)))
            } catch (_: Exception) {
            }

            try {
                val rsaPublicSpec = parsePkcs1RsaPublicKey(keyBytes)
                if (rsaPublicSpec != null) {
                    return JvmRsaPublicKey(keyFactory.generatePublic(rsaPublicSpec))
                }
            } catch (_: Exception) {
            }

            try {
                val cert = certFactory.generateCertificate(ByteArrayInputStream(keyBytes)) as X509Certificate
                return JvmRsaPublicKey(cert.publicKey)
            } catch (_: Exception) {
            }

            val nestedText = try {
                String(keyBytes, Charsets.UTF_8)
            } catch (_: Exception) {
                null
            }
            if (!nestedText.isNullOrBlank() &&
                (nestedText.contains("BEGIN PUBLIC KEY") || nestedText.contains("BEGIN CERTIFICATE"))
            ) {
                return importPublicKeyInternal(nestedText, depth + 1)
            }
        }

        throw IllegalArgumentException("Unsupported public key format")
    }

    private fun parsePkcs1RsaPublicKey(derBytes: ByteArray): RSAPublicKeySpec? {
        if (derBytes.isEmpty()) return null
        var index = 0

        fun readLength(): Int {
            val first = derBytes[index++].toInt() and 0xFF
            if (first and 0x80 == 0) return first
            val byteCount = first and 0x7F
            if (byteCount == 0 || byteCount > 4 || index + byteCount > derBytes.size) {
                throw IllegalArgumentException("Invalid DER length")
            }
            var value = 0
            repeat(byteCount) {
                value = (value shl 8) or (derBytes[index++].toInt() and 0xFF)
            }
            return value
        }

        fun readInteger(): BigInteger {
            if (index >= derBytes.size || derBytes[index++] != 0x02.toByte()) {
                throw IllegalArgumentException("Invalid DER integer")
            }
            val length = readLength()
            if (length <= 0 || index + length > derBytes.size) {
                throw IllegalArgumentException("Invalid DER integer length")
            }
            val value = derBytes.copyOfRange(index, index + length)
            index += length
            return BigInteger(1, value)
        }

        if (derBytes[index++] != 0x30.toByte()) return null
        val sequenceLength = readLength()
        if (sequenceLength <= 0 || index + sequenceLength > derBytes.size) return null

        val modulus = readInteger()
        val exponent = readInteger()
        return RSAPublicKeySpec(modulus, exponent)
    }

    private fun parseOpenSshKey(rawValue: String): RsaPublicKey? {
        val trimmed = rawValue.trim()
        if (!trimmed.startsWith("ssh-rsa ")) return null

        val parts = trimmed.split("\\s+".toRegex(), limit = 3)
        if (parts.size < 2) return null
        val decoded = try {
            base64Codec.decodeDefault(parts[1])
        } catch (_: Exception) {
            return null
        }

        var index = 0
        fun readInt(): Int {
            if (index + 4 > decoded.size) throw IllegalArgumentException("Invalid OpenSSH key")
            val value = ((decoded[index].toInt() and 0xFF) shl 24) or
                ((decoded[index + 1].toInt() and 0xFF) shl 16) or
                ((decoded[index + 2].toInt() and 0xFF) shl 8) or
                (decoded[index + 3].toInt() and 0xFF)
            index += 4
            return value
        }

        fun readBytes(length: Int): ByteArray {
            if (length < 0 || index + length > decoded.size) {
                throw IllegalArgumentException("Invalid OpenSSH key section")
            }
            val value = decoded.copyOfRange(index, index + length)
            index += length
            return value
        }

        return try {
            val type = String(readBytes(readInt()), Charsets.US_ASCII)
            if (type != "ssh-rsa") return null
            val exponent = BigInteger(1, readBytes(readInt()))
            val modulus = BigInteger(1, readBytes(readInt()))
            JvmRsaPublicKey(keyFactory.generatePublic(RSAPublicKeySpec(modulus, exponent)))
        } catch (_: Exception) {
            null
        }
    }

    private fun RsaPublicKey.asJvmKey(): PublicKey {
        return (this as? JvmRsaPublicKey)?.delegate
            ?: keyFactory.generatePublic(X509EncodedKeySpec(encoded))
    }

    private fun RsaPrivateKey.asJvmKey(): PrivateKey {
        return (this as? JvmRsaPrivateKey)?.delegate
            ?: keyFactory.generatePrivate(PKCS8EncodedKeySpec(encoded))
    }

    private fun oaepSha256Spec(): OAEPParameterSpec {
        return OAEPParameterSpec(
            "SHA-256",
            "MGF1",
            MGF1ParameterSpec.SHA256,
            PSource.PSpecified.DEFAULT,
        )
    }

    private companion object {
        const val RSA_ALGORITHM = "RSA"
        const val RSA_OAEP_SHA_256_TRANSFORMATION = "RSA/ECB/OAEPWithSHA-256AndMGF1Padding"
        const val RSA_PKCS1_TRANSFORMATION = "RSA/ECB/PKCS1Padding"
    }
}
