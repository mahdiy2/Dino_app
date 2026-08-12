package com.dino.message.corefeature.domain.crypto

interface Base64Codec {
    fun encodeNoWrap(bytes: ByteArray): String
    fun decodeDefault(value: String): ByteArray
    fun decodeNoWrap(value: String): ByteArray
    fun decodeUrlSafe(value: String, noWrap: Boolean): ByteArray
}
