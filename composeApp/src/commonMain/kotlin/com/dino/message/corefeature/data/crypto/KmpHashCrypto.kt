package com.dino.message.corefeature.data.crypto

import com.dino.message.corefeature.domain.crypto.HashCrypto
import dev.whyoleg.cryptography.CryptographyProvider
import dev.whyoleg.cryptography.algorithms.SHA256

class KmpHashCrypto(
    private val provider: CryptographyProvider = CryptographyProvider.Default,
) : HashCrypto {
    override suspend fun sha256(bytes: ByteArray): ByteArray =
        provider.get(SHA256).hasher().hash(bytes)
}
