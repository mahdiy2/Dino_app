package com.dino.message.corefeature.data.crypto

/** Whether the selected platform provider can decrypt legacy RSA-PKCS#1/AES-ECB payloads. */
expect val platformSupportsLegacyMessageCrypto: Boolean
