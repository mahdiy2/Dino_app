package com.dino.message.corefeature.data.crypto

import com.dino.message.corefeature.domain.crypto.HashCrypto
import java.security.MessageDigest

class JvmHashCrypto : HashCrypto {
    override fun sha256(bytes: ByteArray): ByteArray {
        return MessageDigest.getInstance("SHA-256").digest(bytes)
    }
}
