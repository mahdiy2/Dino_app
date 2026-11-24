package com.dino.message.corefeature.data.crypto

internal expect val requiresPlatformLegacyRsaDecryptor: Boolean

internal expect suspend fun decryptLegacyRsaPkcs1Platform(
    privateKeyPkcs8: ByteArray,
    ciphertext: ByteArray,
): ByteArray
