package com.dino.message.corefeature.domain.crypto

interface HashCrypto {
    fun sha256(bytes: ByteArray): ByteArray
}
