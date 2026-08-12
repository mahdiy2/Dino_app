package com.dino.message.corefeature.data.crypto

import com.dino.message.corefeature.domain.crypto.Base64Codec
import java.util.Base64

class JvmBase64Codec : Base64Codec {
    override fun encodeNoWrap(bytes: ByteArray): String {
        return Base64.getEncoder().encodeToString(bytes)
    }

    override fun decodeDefault(value: String): ByteArray {
        return Base64.getMimeDecoder().decode(value)
    }

    override fun decodeNoWrap(value: String): ByteArray {
        return Base64.getDecoder().decode(value)
    }

    override fun decodeUrlSafe(value: String, noWrap: Boolean): ByteArray {
        return if (noWrap) {
            Base64.getUrlDecoder().decode(value)
        } else {
            Base64.getMimeDecoder().decode(value.replace('-', '+').replace('_', '/'))
        }
    }
}
