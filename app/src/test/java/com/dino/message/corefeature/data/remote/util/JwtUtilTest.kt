package com.dino.message.corefeature.data.remote.util

import com.dino.message.corefeature.data.crypto.JvmBase64Codec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Base64

class JwtUtilTest {
    private val base64Codec = JvmBase64Codec()

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
        val header = Base64.getUrlEncoder()
            .withoutPadding()
            .encodeToString("""{"alg":"none"}""".toByteArray())
        val payload = Base64.getUrlEncoder()
            .withoutPadding()
            .encodeToString(payloadJson.toByteArray())
        return "$header.$payload.signature"
    }
}
