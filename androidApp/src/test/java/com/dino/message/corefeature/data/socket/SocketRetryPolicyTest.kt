package com.dino.message.corefeature.data.socket

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AndroidSocketRetryPolicyTest {

    @Test
    fun `requiresRetry returns true only for chat send events`() {
        assertTrue(AndroidSocketRetryPolicy.requiresRetry("send_message"))
        assertTrue(AndroidSocketRetryPolicy.requiresRetry("send_group_message"))
        assertFalse(AndroidSocketRetryPolicy.requiresRetry("ack_messages"))
    }

    @Test
    fun `autoDedupeKey builds private scope key from payload`() {
        val payload = JSONObject().apply {
            put("to", "alice")
            put("client_message_id", "msg-1")
        }

        assertEquals(
            "private:alice:msg-1",
            AndroidSocketRetryPolicy.autoDedupeKey("send_message", payload),
        )
    }

    @Test
    fun `autoDedupeKey builds group scope key from payload`() {
        val payload = JSONObject().apply {
            put("group_id", 42)
            put("client_message_id", "msg-2")
        }

        assertEquals(
            "group:42:msg-2",
            AndroidSocketRetryPolicy.autoDedupeKey("send_group_message", payload),
        )
    }

    @Test
    fun `autoDedupeKey returns null when client message id is missing`() {
        val payload = JSONObject().apply {
            put("to", "bob")
        }

        assertNull(AndroidSocketRetryPolicy.autoDedupeKey("send_message", payload))
    }

    @Test
    fun `autoDedupeKey returns null for unsupported events`() {
        val payload = JSONObject().apply {
            put("client_message_id", "msg-3")
        }

        assertNull(AndroidSocketRetryPolicy.autoDedupeKey("ack_messages", payload))
    }
}
