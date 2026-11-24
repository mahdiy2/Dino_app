package com.dino.message.corefeature.data.crypto

internal actual val requiresPlatformLegacyRsaDecryptor: Boolean = false

internal actual suspend fun decryptLegacyRsaPkcs1Platform(
    privateKeyPkcs8: ByteArray,
    ciphertext: ByteArray,
): ByteArray = error("The Android JCA provider supplies RSA PKCS#1 decryption")
