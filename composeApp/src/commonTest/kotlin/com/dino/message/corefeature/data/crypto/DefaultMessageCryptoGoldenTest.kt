package com.dino.message.corefeature.data.crypto

import com.dino.message.corefeature.domain.crypto.MessageCrypto
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class DefaultMessageCryptoGoldenTest {
    private val base64Codec = KmpBase64Codec()
    private val messageCrypto: MessageCrypto = DefaultMessageCrypto(
        base64Codec = base64Codec,
        rsaCrypto = KmpRsaCrypto(base64Codec),
        aesGcmCrypto = KmpAesGcmCrypto(),
    )

    @Test
    fun decryptsFixedV2PayloadFromExistingWireFormat() = runTest {
        val privateKey = messageCrypto.decodePrivateKey(PRIVATE_KEY)

        assertEquals(
            "golden private message",
            messageCrypto.decryptMessage(V2_MESSAGE, V2_KEY, privateKey),
        )
    }

    @Test
    fun decryptsFixedLegacyPayload() = runTest {
        if (!platformSupportsLegacyMessageCrypto) return@runTest
        val privateKey = messageCrypto.decodePrivateKey(PRIVATE_KEY)

        assertEquals(
            "legacy golden message",
            messageCrypto.decryptMessage(LEGACY_MESSAGE, LEGACY_KEY, privateKey),
        )
    }

    @Test
    fun privateAndGroupMessagesRoundTripWithV2Prefixes() = runTest {
        val alice = messageCrypto.generateRsaKeyPair()
        val bob = messageCrypto.generateRsaKeyPair()
        val alicePublic = messageCrypto.decodePublicKey(alice.publicKeyBase64)
        val bobPublic = messageCrypto.decodePublicKey(bob.publicKeyBase64)

        val privatePayload = messageCrypto.encryptMessage("private round trip", bobPublic)
        assertTrue(privatePayload.encryptedKey.startsWith("v2:"))
        assertTrue(privatePayload.encryptedMessage.startsWith("v2:"))
        assertEquals(
            "private round trip",
            messageCrypto.decryptMessage(
                privatePayload.encryptedMessage,
                privatePayload.encryptedKey,
                messageCrypto.decodePrivateKey(bob.privateKeyBase64),
            ),
        )

        val groupPayload = messageCrypto.encryptGroupMessage(
            "group round trip",
            mapOf("alice" to alicePublic, "bob" to bobPublic),
        )
        assertEquals(setOf("alice", "bob"), groupPayload.encryptedKeys.keys)
        assertEquals(
            "group round trip",
            messageCrypto.decryptMessage(
                groupPayload.encryptedMessage,
                groupPayload.encryptedKeys.getValue("alice"),
                messageCrypto.decodePrivateKey(alice.privateKeyBase64),
            ),
        )
        val preview = messageCrypto.encryptWithAesKey("reply preview", groupPayload.rawAesKey)
        assertEquals(
            "reply preview",
            messageCrypto.decryptMessage(
                preview,
                groupPayload.encryptedKeys.getValue("bob"),
                messageCrypto.decodePrivateKey(bob.privateKeyBase64),
            ),
        )
    }

    @Test
    fun importsRawPemPkcs1OpenSshUrlSafeAndWrappedPublicKeys() = runTest {
        val raw = messageCrypto.decodePublicKey(PUBLIC_KEY)
        val pem = """
            -----BEGIN PUBLIC KEY-----
            ${PUBLIC_KEY.chunked(64).joinToString("\n")}
            -----END PUBLIC KEY-----
        """.trimIndent()
        val pkcs1Pem = """
            -----BEGIN RSA PUBLIC KEY-----
            ${PKCS1_PUBLIC_KEY.chunked(64).joinToString("\n")}
            -----END RSA PUBLIC KEY-----
        """.trimIndent()
        val urlSafe = PUBLIC_KEY.trimEnd('=').replace('+', '-').replace('/', '_')
        val quotedEscapedPem = "\"${pem.replace("\n", "\\n")}\""
        val nestedPem = base64Codec.encodeNoWrap(pem.encodeToByteArray())
        val dataUriPem = "data:application/x-pem-file;base64,$nestedPem"

        assertContentEquals(raw.encoded, messageCrypto.decodePublicKey(pem).encoded)
        assertContentEquals(raw.encoded, messageCrypto.decodePublicKey(pkcs1Pem).encoded)
        assertContentEquals(raw.encoded, messageCrypto.decodePublicKey(OPEN_SSH_PUBLIC_KEY).encoded)
        assertContentEquals(raw.encoded, messageCrypto.decodePublicKey(urlSafe).encoded)
        assertContentEquals(raw.encoded, messageCrypto.decodePublicKey(quotedEscapedPem).encoded)
        assertContentEquals(raw.encoded, messageCrypto.decodePublicKey(nestedPem).encoded)
        assertContentEquals(raw.encoded, messageCrypto.decodePublicKey(dataUriPem).encoded)
        assertNotNull(messageCrypto.decodePublicKeyOrNull(pem))
    }

    @Test
    fun importsPublicKeyFromX509Certificate() = runTest {
        val expected = messageCrypto.decodePublicKey(CERTIFICATE_PUBLIC_KEY)
        val certificatePem = """
            -----BEGIN CERTIFICATE-----
            ${CERTIFICATE.chunked(64).joinToString("\n")}
            -----END CERTIFICATE-----
        """.trimIndent()

        assertContentEquals(expected.encoded, messageCrypto.decodePublicKey(certificatePem).encoded)
    }


    private companion object {
        const val PUBLIC_KEY = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAqtQrZfY4Rsd364lNZwtB4dMjWbsZAEbaM1QCn6tTaT/1Ra3hbPh+Y29YFdFscDOM/87gW4ptGk0ENybH2IBCbiigMVQk6n9txRDZQpmpDO6cXObv4ViMugoMICmt3vKG14ceGoIJCY2qW4xXBlQ7rbxH7Iu+bP+v7eSCPRqdejRO+W3yyRwq61FI0FRbBiFIDY6+kks0LcKvYNcvA38ow9yBzfeUlgk6eSfxDGqmTlzM+FdS435nbcoQDKC4vRZlCy7Z9rK592mYiVIUn+yGaAWwvcUntkHFuAxk0G2MD0oo/+aQDUJyvQK9YxpWwq9W6xUNBvxRMEPH0D/THJGrMQIDAQAB"
        const val PKCS1_PUBLIC_KEY = "MIIBCgKCAQEAqtQrZfY4Rsd364lNZwtB4dMjWbsZAEbaM1QCn6tTaT/1Ra3hbPh+Y29YFdFscDOM/87gW4ptGk0ENybH2IBCbiigMVQk6n9txRDZQpmpDO6cXObv4ViMugoMICmt3vKG14ceGoIJCY2qW4xXBlQ7rbxH7Iu+bP+v7eSCPRqdejRO+W3yyRwq61FI0FRbBiFIDY6+kks0LcKvYNcvA38ow9yBzfeUlgk6eSfxDGqmTlzM+FdS435nbcoQDKC4vRZlCy7Z9rK592mYiVIUn+yGaAWwvcUntkHFuAxk0G2MD0oo/+aQDUJyvQK9YxpWwq9W6xUNBvxRMEPH0D/THJGrMQIDAQAB"
        const val PRIVATE_KEY = "MIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQCq1Ctl9jhGx3friU1nC0Hh0yNZuxkARtozVAKfq1NpP/VFreFs+H5jb1gV0WxwM4z/zuBbim0aTQQ3JsfYgEJuKKAxVCTqf23FENlCmakM7pxc5u/hWIy6CgwgKa3e8obXhx4aggkJjapbjFcGVDutvEfsi75s/6/t5II9Gp16NE75bfLJHCrrUUjQVFsGIUgNjr6SSzQtwq9g1y8DfyjD3IHN95SWCTp5J/EMaqZOXMz4V1LjfmdtyhAMoLi9FmULLtn2srn3aZiJUhSf7IZoBbC9xSe2QcW4DGTQbYwPSij/5pANQnK9Ar1jGlbCr1brFQ0G/FEwQ8fQP9MckasxAgMBAAECggEAVJnj7kicwUza8n6aozwbgiPHH5K9xHnSaVYjQIEgO4EtzrbY69c6/LKxXIjNW9nj6/JAAYXSF5Lp0gh2M539iEG9aACkka/PJKf9+PJRmvJ25LmINkFc3GvKaolJrx7MMeQcQAODR0RuCI29sYg40AdtVVQ12BRexAGFkI5kTBbZAufqUzhbZ/6MXCeH+aNPluQHgFFPANX3wSt9rt5IErLbyzxGG+f43nPIY5zU8QDOFTI4hCi/uKqJaHyZU5JABxcf87G9obHeq9ZSdVczc/w6K4dYOXRc4eziwu9AtcDj+kRPZjChjiRevcmsvZhmJvUJd7/oscwAbooRvzUdGwKBgQDhtFtRmBGrHsiA8vA5/msuiTX6dZZztPmLQ16mU9vkn97L2rtcDVCvJMqM5qNPvYnfWXxVQCsBxYglaFCBQw/frOmmYIbef6Iv9VgEgQnsq5WjeKtPKLBPqkFYSJyFiR36zYDdpC9TXxXkliUx5qiNUE7AJcBjeQY2CoS1jyCtUwKBgQDBwi06dcNgdpKtPTB7FISx1JzdDLMuAQ3SEolvG6xAH0BQDA59jlUJnttfDJSS6wb0DoQezkhoJn6cMlzksz2bOcgZDPSLNJTf3dzihZc5t2Rpol3JYw/X8XCC0w4Y28dPQYj15U5kHK01KWlOfs+o85kU/KPex3qyEkBVLUEw6wKBgF3OPd3tm/pI8wBU9+7ChP10XZDY6kzwvH5a462xUMT2hPyozhH2HsCMQ/rHqLxthC3l3Tz5TGaSGIGjl0ICA0jtbraNi8BRv7C7AXftwZgxwCGO93QQ7yZNFpeApj0BvgoM+zAjGuBFtq2okZDp0sfYxsIpboppNGGlgX6MHAKtAoGBAIjzKGizp/7ckFvmbgfwvsG614zeVx9jG1OqSn2//4XrENgDTZKtMan1IHUUPUeC241RXU0w3LjQzJi2eDYmAptewfIC2C40Yi3BcUiix/rBhzWqz8NyQ1aMjsvJRaOIZF2+ooG7LCdSpZJ3zLjwpPJ0C3VsBJ+IO07124bYz8U1AoGAC3XThacIEt8TFLhIDOapSTLtsiDEhLZZvRvHQ29UCGPIxw2czp0+HTLrALCl5LkXpMWulBcK392nmYD4Emlfywv5dwLX2k/DbQQL3KODmQmaIGYo0u9X5iyqgUJB3gdT8AW19A6BMmeZVQ7W703V++AKnAIrZmsdEmozpBChjrc="
        const val V2_MESSAGE = "v2:oKGio6Slpqeoqaqr3r5RApjbenQNZ9QKheAA6TKkw3g/DV840GNSdGYfcbRaiLs8D6Q="
        const val V2_KEY = "v2:FVPBAsLutADw+nwUkDFudh5NsRE7I3KAAD5UqdhZluHlneRcaZtXybzuK1hVK02ZpE5SxeBGQj0zpcUtoo5go790FG5dUq51DuBwQo8/jXrSAz6H2ofWa7RHEqj6qgiuIzfBfP33by80s9K/hPj3KD1DyvXVKyxpBlYfy6t024nvVRvjsLFo18Kg/NrrzIv/EYcKF+BxEEIMWcnFWpoNp1JFImIJMd9qTW0MiEGOcS12YDwPfRMn9oaw7yLZ/kdMdqFGzVRzaOdRrY+jbZp2BdCbPXpzGC1tjEr8X3yxNOxIb+VbvbbDIARDQ5OE3503dFK4Cjn/qDukoGCEg4zDYQ=="
        const val LEGACY_MESSAGE = "cCqjvnOfnrHhOkSt07I7Z25Ye2ayDzJ1heKw5B3roJw="
        const val LEGACY_KEY = "P9qpV6mMTUZbrDpRxMBAOYL9pk+vY30VRKHKvitdaHfYhHN1y27MUoc7SRVRtMctLUfgvLCzczEwuMqZ/AFM1rJzc3I2R0+bRmXqhlTiPmh2SpUP8qzWFwoezjiBQD65YQYgesJRZvqiu8GUR9zpsRQeF1WYn+Cx0F+3GgEWAOOefz7bKeAU6LnefXMkcdMqD1OYkwyQ/zwvUpkfcD8vrLq7rVT2g8gDhVEu4qPpru7oB3C4RcLLCcm4knxV8bkgxQFdeiSoeV1/GvKjBawOXB/NUhUIiGruFf59G1tFgM/we5YcglrZqKLnuB5TOkkEFEESj5OE/YC4DEI+O/0lPQ=="
        const val OPEN_SSH_PUBLIC_KEY = "ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAABAKrUK2X2OEbHd+uJTWcLQeHTI1m7GQBG2jNUAp+rU2k/9UWt4Wz4fmNvWBXRbHAzjP/O4FuKbRpNBDcmx9iAQm4ooDFUJOp/bcUQ2UKZqQzunFzm7+FYjLoKDCAprd7yhteHHhqCCQmNqluMVwZUO628R+yLvmz/r+3kgj0anXo0Tvlt8skcKutRSNBUWwYhSA2OvpJLNC3Cr2DXLwN/KMPcgc33lJYJOnkn8Qxqpk5czPhXUuN+Z23KEAyguL0WZQsu2fayufdpmIlSFJ/shmgFsL3FJ7ZBxbgMZNBtjA9KKP/mkA1Ccr0CvWMaVsKvVusVDQb8UTBDx9A/0xyRqzE= golden-vector"
        const val CERTIFICATE_PUBLIC_KEY = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAiYQrwMOaLxJWSbqs5cFLcja9sDJc/Cs9sEcZZfY9OS+1wDriKs5LvsqKvsy0Fv1wZp6WxtPntJXF+Zh4KcghUMsELhUYtvcRZjn520cfhz0Hfp/8X/3cXq/qZvOvJ4VguwCqOjpazbgc+fwTPidCzoMzvx13++cVY+UIu+vDDl29/uA4e6hpQa1pcYb3AyOAFLG0ixsURT38XPOHR62TkK9+0CNo2zqxrV+EVvDqrQn2fPl/b0JcajNv1q1VP70oJB/qtZ07Sn7nwawMys5aH7KF3KtjhT2SspCOPmbT5ZHEBfnhj8yU5o/juf2aS6ahHZtsFhwHAuCofNXb8Mku7QIDAQAB"
        const val CERTIFICATE = "MIIDFzCCAf+gAwIBAgIUC6FKAMN4Yel5PHv2ucfp9NqwYpYwDQYJKoZIhvcNAQELBQAwGzEZMBcGA1UEAwwQc2VjLW1lc3NhZ2UtdGVzdDAeFw0yNjA5MTQxNjQ3MTZaFw0zNjA5MTExNjQ3MTZaMBsxGTAXBgNVBAMMEHNlYy1tZXNzYWdlLXRlc3QwggEiMA0GCSqGSIb3DQEBAQUAA4IBDwAwggEKAoIBAQCJhCvAw5ovElZJuqzlwUtyNr2wMlz8Kz2wRxll9j05L7XAOuIqzku+yoq+zLQW/XBmnpbG0+e0lcX5mHgpyCFQywQuFRi29xFmOfnbRx+HPQd+n/xf/dxer+pm868nhWC7AKo6OlrNuBz5/BM+J0LOgzO/HXf75xVj5Qi768MOXb3+4Dh7qGlBrWlxhvcDI4AUsbSLGxRFPfxc84dHrZOQr37QI2jbOrGtX4RW8OqtCfZ8+X9vQlxqM2/WrVU/vSgkH+q1nTtKfufBrAzKzlofsoXcq2OFPZKykI4+ZtPlkcQF+eGPzJTmj+O5/ZpLpqEdm2wWHAcC4Kh81dvwyS7tAgMBAAGjUzBRMB0GA1UdDgQWBBTXGwRUOOXsTcnRwxbkm61DkQP5TjAfBgNVHSMEGDAWgBTXGwRUOOXsTcnRwxbkm61DkQP5TjAPBgNVHRMBAf8EBTADAQH/MA0GCSqGSIb3DQEBCwUAA4IBAQBfpusM2xBOAXVMc1cikRNKh4vYwZJm/LEc2zgeorz3pjAEgF3m20UqTOZaQdAOS2isRi3YfV2wmFX1zHSAL8Jg6GDfbk8JhQOPxAVue7s+Po89kSRk18YmA6i35cuGFazFHpUR6SvGU2nSn7Is3csR5AsVtYPiFblLTOdnXRGcNsLH8bh8+/3OPD+IMXxH+lXQisEyXP89Rk36Z9TgcplokBqYb2vkCUDyKx/IwbbPxopCzAOgzn5a7iONv0XwhmTVe5oVHApSBhh6Z/oKpCAm/HtVqR313dPpa893QRwpPpqcKdhqP8LCstSMV9WsVQRMRXrKmPguJoYnigmcJ5Tv"
    }
}
