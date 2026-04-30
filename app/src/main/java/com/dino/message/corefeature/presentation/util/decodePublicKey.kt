package com.dino.message.corefeature.presentation.util

import android.util.Base64
import android.util.Log
import java.io.ByteArrayInputStream
import java.math.BigInteger
import java.security.KeyFactory
import java.security.PublicKey
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.security.spec.RSAPublicKeySpec
import java.security.spec.X509EncodedKeySpec

private const val TAG = "DecodePublicKey"

fun decodePublicKey(base64Key: String): PublicKey {
    return decodePublicKeyInternal(base64Key, depth = 0)
}

fun decodePublicKeyOrNull(base64Key: String?): PublicKey? {
    if (base64Key.isNullOrBlank()) return null
    return try {
        decodePublicKeyInternal(base64Key, depth = 0)
    } catch (_: Exception) {
        null
    }
}

private fun decodePublicKeyInternal(rawValue: String, depth: Int): PublicKey {
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

    val decodeFlags = intArrayOf(
        Base64.DEFAULT,
        Base64.NO_WRAP,
        Base64.URL_SAFE,
        Base64.URL_SAFE or Base64.NO_WRAP
    )

    val keyFactory = KeyFactory.getInstance("RSA")
    val certFactory = CertificateFactory.getInstance("X.509")

    for (flag in decodeFlags) {
        val keyBytes = try {
            Base64.decode(padded, flag)
        } catch (_: IllegalArgumentException) {
            continue
        }

        try {
            val keySpec = X509EncodedKeySpec(keyBytes)
            return keyFactory.generatePublic(keySpec)
        } catch (_: Exception) {
        }

        try {
            val rsaPublicSpec = parsePkcs1RsaPublicKey(keyBytes)
            if (rsaPublicSpec != null) {
                return keyFactory.generatePublic(rsaPublicSpec)
            }
        } catch (_: Exception) {
        }

        try {
            val cert = certFactory.generateCertificate(ByteArrayInputStream(keyBytes)) as X509Certificate
            return cert.publicKey
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
            return decodePublicKeyInternal(nestedText, depth + 1)
        }
    }

    Log.w(TAG, "Unsupported public key format at depth=$depth, ${formatPublicKeyForLog(rawValue)}")
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

private fun parseOpenSshKey(rawValue: String): PublicKey? {
    val trimmed = rawValue.trim()
    if (!trimmed.startsWith("ssh-rsa ")) return null

    val parts = trimmed.split("\\s+".toRegex(), limit = 3)
    if (parts.size < 2) return null
    val keyBlob = parts[1]
    val decoded = try {
        Base64.decode(keyBlob, Base64.DEFAULT)
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
        val spec = RSAPublicKeySpec(modulus, exponent)
        KeyFactory.getInstance("RSA").generatePublic(spec)
    } catch (_: Exception) {
        null
    }
}
