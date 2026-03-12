package com.dino.message.corefeature.presentation.util

import android.util.Base64
import java.nio.charset.StandardCharsets
import java.security.PrivateKey
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.OAEPParameterSpec
import javax.crypto.spec.PSource
import javax.crypto.spec.SecretKeySpec

private const val V2_PREFIX = "v2:"
private const val GCM_IV_SIZE_BYTES = 12
private const val GCM_TAG_LENGTH_BITS = 128

fun decryptMessage(
    encryptedMessage: String,
    encryptedAESKey: String,
    privateKey: PrivateKey
): String {
    return if (encryptedMessage.startsWith(V2_PREFIX) && encryptedAESKey.startsWith(V2_PREFIX)) {
        decryptV2(
            encryptedMessage = encryptedMessage.removePrefix(V2_PREFIX),
            encryptedAESKey = encryptedAESKey.removePrefix(V2_PREFIX),
            privateKey = privateKey
        )
    } else {
        decryptLegacy(
            encryptedMessage = encryptedMessage,
            encryptedAESKey = encryptedAESKey,
            privateKey = privateKey
        )
    }
}

private fun decryptV2(
    encryptedMessage: String,
    encryptedAESKey: String,
    privateKey: PrivateKey
): String {
    val rsaCipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding")
    rsaCipher.init(
        Cipher.DECRYPT_MODE,
        privateKey,
        OAEPParameterSpec("SHA-256", "MGF1", java.security.spec.MGF1ParameterSpec.SHA256, PSource.PSpecified.DEFAULT)
    )
    val aesKey = rsaCipher.doFinal(Base64.decode(encryptedAESKey, Base64.NO_WRAP))

    val encryptedMessageWithIv = Base64.decode(encryptedMessage, Base64.NO_WRAP)
    require(encryptedMessageWithIv.size > GCM_IV_SIZE_BYTES) { "Invalid encrypted payload" }

    val iv = encryptedMessageWithIv.copyOfRange(0, GCM_IV_SIZE_BYTES)
    val cipherText = encryptedMessageWithIv.copyOfRange(GCM_IV_SIZE_BYTES, encryptedMessageWithIv.size)

    val aesCipher = Cipher.getInstance("AES/GCM/NoPadding")
    val secretKey = SecretKeySpec(aesKey, "AES")
    aesCipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv))
    val decryptedBytes = aesCipher.doFinal(cipherText)

    return String(decryptedBytes, StandardCharsets.UTF_8)
}

private fun decryptLegacy(
    encryptedMessage: String,
    encryptedAESKey: String,
    privateKey: PrivateKey
): String {
    val rsaCipher = Cipher.getInstance("RSA/ECB/PKCS1Padding")
    rsaCipher.init(Cipher.DECRYPT_MODE, privateKey)
    val aesKey = rsaCipher.doFinal(Base64.decode(encryptedAESKey, Base64.NO_WRAP))

    val aesCipher = Cipher.getInstance("AES")
    val secretKey = SecretKeySpec(aesKey, "AES")
    aesCipher.init(Cipher.DECRYPT_MODE, secretKey)
    val decryptedBytes = aesCipher.doFinal(Base64.decode(encryptedMessage, Base64.NO_WRAP))

    return String(decryptedBytes, StandardCharsets.UTF_8)
}
