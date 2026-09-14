package com.dino.message.corefeature.domain.crypto

interface HashCrypto {
    suspend fun sha256(bytes: ByteArray): ByteArray
}
