package com.dino.message.corefeature.data.crypto

internal actual val requiresPlatformLegacyRsaDecryptor: Boolean = true

internal actual suspend fun decryptLegacyRsaPkcs1Platform(
    privateKeyPkcs8: ByteArray,
    ciphertext: ByteArray,
): ByteArray {
    val privateKey = parsePkcs8RsaPrivateKey(privateKeyPkcs8)
    val encodedMessageHex = modularExponentiationHex(
        baseHex = ciphertext.toUnsignedHex(),
        exponentHex = privateKey.privateExponent.toUnsignedHex(),
        modulusHex = privateKey.modulus.toUnsignedHex(),
    )
    val encodedMessage = encodedMessageHex.hexToBytes(privateKey.modulus.size)
    require(encodedMessage.size >= 11 && encodedMessage[0] == 0.toByte() && encodedMessage[1] == 2.toByte()) {
        "Invalid RSA PKCS#1 payload"
    }
    val separator = (2 until encodedMessage.size).firstOrNull { encodedMessage[it] == 0.toByte() }
        ?: throw IllegalArgumentException("Invalid RSA PKCS#1 padding")
    require(separator >= 10) { "Invalid RSA PKCS#1 padding" }
    return encodedMessage.copyOfRange(separator + 1, encodedMessage.size)
}

private data class ParsedRsaPrivateKey(
    val modulus: ByteArray,
    val privateExponent: ByteArray,
)

private fun parsePkcs8RsaPrivateKey(pkcs8: ByteArray): ParsedRsaPrivateKey {
    val outer = readTlv(pkcs8, 0)
    require(outer.tag == DER_SEQUENCE) { "Invalid PKCS#8 key" }
    var cursor = outer.contentStart
    cursor = readTlv(pkcs8, cursor).end // version
    cursor = readTlv(pkcs8, cursor).end // algorithm identifier
    val privateKeyOctets = readTlv(pkcs8, cursor)
    require(privateKeyOctets.tag == DER_OCTET_STRING) { "Invalid PKCS#8 key" }

    val pkcs1 = pkcs8.copyOfRange(privateKeyOctets.contentStart, privateKeyOctets.end)
    val sequence = readTlv(pkcs1, 0)
    require(sequence.tag == DER_SEQUENCE) { "Invalid RSA private key" }
    var pkcs1Cursor = sequence.contentStart
    pkcs1Cursor = readTlv(pkcs1, pkcs1Cursor).end // version
    val modulus = readInteger(pkcs1, pkcs1Cursor).also { pkcs1Cursor = it.second }.first
    pkcs1Cursor = readTlv(pkcs1, pkcs1Cursor).end // public exponent
    val privateExponent = readInteger(pkcs1, pkcs1Cursor).first
    return ParsedRsaPrivateKey(modulus.stripLeadingZero(), privateExponent.stripLeadingZero())
}

private data class DerTlv(val tag: Int, val contentStart: Int, val end: Int)

private fun readTlv(bytes: ByteArray, start: Int): DerTlv {
    require(start + 2 <= bytes.size) { "Invalid DER value" }
    val tag = bytes[start].toInt() and 0xff
    var cursor = start + 1
    val firstLength = bytes[cursor++].toInt() and 0xff
    val length = if (firstLength and 0x80 == 0) {
        firstLength
    } else {
        val count = firstLength and 0x7f
        require(count in 1..4 && cursor + count <= bytes.size) { "Invalid DER length" }
        var parsed = 0
        repeat(count) { parsed = (parsed shl 8) or (bytes[cursor++].toInt() and 0xff) }
        parsed
    }
    require(cursor + length <= bytes.size) { "Invalid DER length" }
    return DerTlv(tag, cursor, cursor + length)
}

private fun readInteger(bytes: ByteArray, start: Int): Pair<ByteArray, Int> {
    val tlv = readTlv(bytes, start)
    require(tlv.tag == DER_INTEGER) { "Invalid RSA integer" }
    return bytes.copyOfRange(tlv.contentStart, tlv.end) to tlv.end
}

private fun ByteArray.stripLeadingZero(): ByteArray =
    if (size > 1 && first() == 0.toByte()) copyOfRange(1, size) else this

private fun ByteArray.toUnsignedHex(): String = joinToString("") {
    val value = it.toInt() and 0xff
    HEX[value ushr 4].toString() + HEX[value and 0x0f]
}

private fun String.hexToBytes(size: Int): ByteArray {
    val normalized = padStart(size * 2, '0')
    require(normalized.length == size * 2) { "Invalid RSA result size" }
    return ByteArray(size) { index -> normalized.substring(index * 2, index * 2 + 2).toInt(16).toByte() }
}

private fun modularExponentiationHex(baseHex: String, exponentHex: String, modulusHex: String): String = js(
    """(() => {
        let base = BigInt('0x' + baseHex) % BigInt('0x' + modulusHex);
        let exponent = BigInt('0x' + exponentHex);
        const modulus = BigInt('0x' + modulusHex);
        let result = 1n;
        while (exponent > 0n) {
            if ((exponent & 1n) === 1n) result = (result * base) % modulus;
            exponent >>= 1n;
            base = (base * base) % modulus;
        }
        return result.toString(16);
    })()"""
)

private const val DER_SEQUENCE = 0x30
private const val DER_INTEGER = 0x02
private const val DER_OCTET_STRING = 0x04
private const val HEX = "0123456789abcdef"
