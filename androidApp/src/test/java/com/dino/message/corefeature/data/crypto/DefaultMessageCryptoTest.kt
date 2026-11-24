package com.dino.message.corefeature.data.crypto

import com.dino.message.corefeature.domain.crypto.MessageCrypto
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.math.BigInteger
import java.security.KeyFactory
import java.security.interfaces.RSAPublicKey
import java.security.spec.X509EncodedKeySpec
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

class DefaultMessageCryptoTest {
    private val base64Codec = JvmBase64Codec()
    private val rsaCrypto = JvmRsaCrypto(base64Codec)
    private val messageCrypto: MessageCrypto = DefaultMessageCrypto(
        base64Codec = base64Codec,
        rsaCrypto = rsaCrypto,
        aesGcmCrypto = JvmAesGcmCrypto(),
    )

    @Test
    fun privateMessageRoundTripUsesV2Payloads() = runTest {
        val keyPair = messageCrypto.generateRsaKeyPair()
        val publicKey = messageCrypto.decodePublicKey(keyPair.publicKeyBase64)
        val privateKey = messageCrypto.decodePrivateKey(keyPair.privateKeyBase64)

        val encrypted = messageCrypto.encryptMessage("hello secure world", publicKey)
        val decrypted = messageCrypto.decryptMessage(
            encryptedMessage = encrypted.encryptedMessage,
            encryptedAesKey = encrypted.encryptedKey,
            privateKey = privateKey,
        )

        assertTrue(encrypted.encryptedMessage.startsWith("v2:"))
        assertTrue(encrypted.encryptedKey.startsWith("v2:"))
        assertEquals("hello secure world", decrypted)
    }

    @Test
    fun groupMessageRoundTripDecryptsForEveryRecipient() = runTest {
        val alice = messageCrypto.generateRsaKeyPair()
        val bob = messageCrypto.generateRsaKeyPair()
        val members = mapOf(
            "alice" to messageCrypto.decodePublicKey(alice.publicKeyBase64),
            "bob" to messageCrypto.decodePublicKey(bob.publicKeyBase64),
        )

        val encrypted = messageCrypto.encryptGroupMessage("group payload", members)

        assertEquals(setOf("alice", "bob"), encrypted.encryptedKeys.keys)
        assertEquals(
            "group payload",
            messageCrypto.decryptMessage(
                encryptedMessage = encrypted.encryptedMessage,
                encryptedAesKey = encrypted.encryptedKeys.getValue("alice"),
                privateKey = messageCrypto.decodePrivateKey(alice.privateKeyBase64),
            ),
        )
        assertEquals(
            "group payload",
            messageCrypto.decryptMessage(
                encryptedMessage = encrypted.encryptedMessage,
                encryptedAesKey = encrypted.encryptedKeys.getValue("bob"),
                privateKey = messageCrypto.decodePrivateKey(bob.privateKeyBase64),
            ),
        )
    }

    @Test
    fun existingGroupAesKeyEncryptsReplyPreview() = runTest {
        val keyPair = messageCrypto.generateRsaKeyPair()
        val encryptedGroup = messageCrypto.encryptGroupMessage(
            message = "",
            memberPublicKeys = mapOf("me" to messageCrypto.decodePublicKey(keyPair.publicKeyBase64)),
        )

        val encryptedPreview = messageCrypto.encryptWithAesKey(
            message = "reply preview",
            aesKey = encryptedGroup.rawAesKey,
        )

        assertEquals(
            "reply preview",
            messageCrypto.decryptMessage(
                encryptedMessage = encryptedPreview,
                encryptedAesKey = encryptedGroup.encryptedKeys.getValue("me"),
                privateKey = messageCrypto.decodePrivateKey(keyPair.privateKeyBase64),
            ),
        )
    }

    @Test
    fun legacyPayloadsStillDecrypt() = runTest {
        val keyPair = messageCrypto.generateRsaKeyPair()
        val privateKey = messageCrypto.decodePrivateKey(keyPair.privateKeyBase64)
        val publicKey = KeyFactory.getInstance("RSA")
            .generatePublic(X509EncodedKeySpec(Base64.getDecoder().decode(keyPair.publicKeyBase64)))

        val aesKey = ByteArray(32) { index -> (index + 1).toByte() }
        val aesCipher = Cipher.getInstance("AES")
        aesCipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(aesKey, "AES"))
        val encryptedMessage = Base64.getEncoder().encodeToString(aesCipher.doFinal("legacy text".toByteArray()))

        val rsaCipher = Cipher.getInstance("RSA/ECB/PKCS1Padding")
        rsaCipher.init(Cipher.ENCRYPT_MODE, publicKey)
        val encryptedKey = Base64.getEncoder().encodeToString(rsaCipher.doFinal(aesKey))

        assertEquals(
            "legacy text",
            messageCrypto.decryptMessage(
                encryptedMessage = encryptedMessage,
                encryptedAesKey = encryptedKey,
                privateKey = privateKey,
            ),
        )
    }

    @Test
    fun publicKeyDecoderAcceptsX509PemAndOpenSsh() = runTest {
        val keyPair = messageCrypto.generateRsaKeyPair()
        val x509 = messageCrypto.decodePublicKey(keyPair.publicKeyBase64)
        val pem = """
            -----BEGIN PUBLIC KEY-----
            ${keyPair.publicKeyBase64.chunked(64).joinToString("\n")}
            -----END PUBLIC KEY-----
        """.trimIndent()
        val openSsh = "ssh-rsa ${toOpenSshBlob(x509.encoded)} generated-test-key"

        assertEquals(x509.encoded.toList(), messageCrypto.decodePublicKey(pem).encoded.toList())
        assertEquals(x509.encoded.toList(), messageCrypto.decodePublicKey(openSsh).encoded.toList())
        assertNotNull(messageCrypto.decodePublicKeyOrNull(pem))
    }


    private fun toOpenSshBlob(x509Encoded: ByteArray): String {
        val publicKey = KeyFactory.getInstance("RSA")
            .generatePublic(X509EncodedKeySpec(x509Encoded)) as RSAPublicKey
        val blob = ByteArrayOutputStream().apply {
            writeSshBytes("ssh-rsa".toByteArray(Charsets.US_ASCII))
            writeSshBytes(publicKey.publicExponent.toUnsignedBytes())
            writeSshBytes(publicKey.modulus.toUnsignedBytes())
        }.toByteArray()
        return Base64.getEncoder().encodeToString(blob)
    }

    private fun ByteArrayOutputStream.writeSshBytes(bytes: ByteArray) {
        write(byteArrayOf(
            (bytes.size ushr 24).toByte(),
            (bytes.size ushr 16).toByte(),
            (bytes.size ushr 8).toByte(),
            bytes.size.toByte(),
        ))
        write(bytes)
    }

    private fun BigInteger.toUnsignedBytes(): ByteArray {
        val bytes = toByteArray()
        return if (bytes.size > 1 && bytes[0] == 0.toByte()) {
            bytes.copyOfRange(1, bytes.size)
        } else {
            bytes
        }
    }
}
