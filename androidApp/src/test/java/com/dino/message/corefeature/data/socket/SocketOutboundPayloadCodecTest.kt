package com.dino.message.corefeature.data.socket

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AndroidSocketOutboundPayloadCodecTest {

    @Test
    fun encodeDecode_jsonObject_roundTrips() {
        val payload = JSONObject().apply {
            put("to", "alice")
            put("message", "enc")
        }

        val encoded = AndroidSocketOutboundPayloadCodec.encode(payload)
        val decoded = AndroidSocketOutboundPayloadCodec.decode(encoded.payloadType, encoded.payloadBody)

        assertTrue(decoded is JSONObject)
        assertEquals("alice", (decoded as JSONObject).optString("to"))
    }

    @Test
    fun encodeDecode_jsonArray_roundTrips() {
        val payload = JSONArray().apply {
            put("a")
            put("b")
        }

        val encoded = AndroidSocketOutboundPayloadCodec.encode(payload)
        val decoded = AndroidSocketOutboundPayloadCodec.decode(encoded.payloadType, encoded.payloadBody)

        assertTrue(decoded is JSONArray)
        assertEquals(2, (decoded as JSONArray).length())
    }

    @Test
    fun encodeDecode_null_roundTrips() {
        val encoded = AndroidSocketOutboundPayloadCodec.encode(null)
        val decoded = AndroidSocketOutboundPayloadCodec.decode(encoded.payloadType, encoded.payloadBody)

        assertNull(decoded)
    }
}
