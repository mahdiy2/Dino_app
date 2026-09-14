package com.dino.message.corefeature.data.crypto

import com.dino.message.corefeature.domain.crypto.MessageCrypto

/** Creates the platform-provider-backed crypto implementation for non-Android KMP entry points. */
fun createPlatformMessageCrypto(): MessageCrypto {
    val base64Codec = KmpBase64Codec()
    return DefaultMessageCrypto(
        base64Codec = base64Codec,
        rsaCrypto = KmpRsaCrypto(base64Codec),
        aesGcmCrypto = KmpAesGcmCrypto(),
        hashCrypto = KmpHashCrypto(),
    )
}
