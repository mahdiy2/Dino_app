package com.dino.message.corefeature.data.crypto

internal actual suspend fun decryptLegacyRsaPkcs1Platform(
    privateKeyPkcs8: ByteArray,
    ciphertext: ByteArray,
): ByteArray = error("The iOS OpenSSL provider supplies RSA PKCS#1 decryption")
