package com.dino.message.corefeature.data.crypto

internal expect suspend fun decryptLegacyRsaPkcs1Platform(
    privateKeyPkcs8: ByteArray,
    ciphertext: ByteArray,
): ByteArray
