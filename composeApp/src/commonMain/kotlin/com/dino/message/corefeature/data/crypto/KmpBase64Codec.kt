package com.dino.message.corefeature.data.crypto

import com.dino.message.corefeature.domain.crypto.Base64Codec
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

@OptIn(ExperimentalEncodingApi::class)
class KmpBase64Codec : Base64Codec {
    override fun encodeNoWrap(bytes: ByteArray): String = Base64.Default.encode(bytes)

    override fun decodeDefault(value: String): ByteArray = Base64.Default.decode(normalize(value))

    override fun decodeNoWrap(value: String): ByteArray = Base64.Default.decode(pad(value.trim()))

    override fun decodeUrlSafe(value: String, noWrap: Boolean): ByteArray =
        Base64.UrlSafe.decode(pad(if (noWrap) value.trim() else normalize(value)))

    private fun normalize(value: String): String = value.filterNot(Char::isWhitespace).let(::pad)

    private fun pad(value: String): String = when (value.length % 4) {
        2 -> "$value=="
        3 -> "$value="
        else -> value
    }
}
