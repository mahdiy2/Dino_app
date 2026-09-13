package com.dino.message.corefeature.data.remote.util

import com.dino.message.corefeature.domain.crypto.Base64Codec
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

@OptIn(ExperimentalEncodingApi::class)
class JwtUtilTest {
    private val base64Codec = object : Base64Codec {
        override fun encodeNoWrap(bytes: ByteArray): String = Base64.Default.encode(bytes).trimEnd('=')
        override fun decodeDefault(value: String): ByteArray = Base64.Default.decode(value)
        override fun decodeNoWrap(value: String): ByteArray = Base64.Default.decode(value)
        override fun decodeUrlSafe(value: String, noWrap: Boolean): ByteArray = Base64.UrlSafe.decode(value)
    }

    @Test
    fun decodeJwtExpirationEpochSecondsReadsUrlSafePayload() {
        val token = jwtWithPayload("""{"sub":"user-1","exp":1234567890}""")

        assertEquals(1234567890L, decodeJwtExpirationEpochSeconds(token, base64Codec))
    }

    @Test
    fun decodeJwtExpirationEpochSecondsReturnsNullWhenExpIsMissing() {
        val token = jwtWithPayload("""{"sub":"user-1"}""")

        assertNull(decodeJwtExpirationEpochSeconds(token, base64Codec))
    }

    @Test
    fun decodeJwtExpirationEpochSecondsReturnsNullForMalformedToken() {
        assertNull(decodeJwtExpirationEpochSeconds("not-a-jwt", base64Codec))
        assertNull(decodeJwtExpirationEpochSeconds("header.invalid!.signature", base64Codec))
    }

    private fun jwtWithPayload(payloadJson: String): String {
        val header = Base64.UrlSafe.encode("""{"alg":"none"}""".encodeToByteArray()).trimEnd('=')
        val payload = Base64.UrlSafe.encode(payloadJson.encodeToByteArray()).trimEnd('=')
        return "$header.$payload.signature"
    }
}
