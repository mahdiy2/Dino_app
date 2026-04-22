package com.dino.message.chatfeature.presentation.viewmodel

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PrivateAckScopeTest {

    @Test
    fun `ackableMessageId returns id for active chat sender`() {
        val payload = JSONObject(
            """
            {
              "from": "alice",
              "message_id": "msg-1"
            }
            """.trimIndent()
        )

        val result = PrivateAckScope.ackableMessageId(
            payload = payload,
            chatId = "alice",
            currentlyActiveChatId = "alice",
        )

        assertEquals("msg-1", result)
    }

    @Test
    fun `ackableMessageId returns null for different sender`() {
        val payload = JSONObject(
            """
            {
              "from": "bob",
              "message_id": "msg-2"
            }
            """.trimIndent()
        )

        val result = PrivateAckScope.ackableMessageId(
            payload = payload,
            chatId = "alice",
            currentlyActiveChatId = "alice",
        )

        assertNull(result)
    }

    @Test
    fun `ackableMessageId returns null for blank message id`() {
        val payload = JSONObject(
            """
            {
              "from": "alice",
              "message_id": "   "
            }
            """.trimIndent()
        )

        val result = PrivateAckScope.ackableMessageId(
            payload = payload,
            chatId = "alice",
            currentlyActiveChatId = "alice",
        )

        assertNull(result)
    }

    @Test
    fun `ackableMessageId returns null when view model chat is no longer active`() {
        val payload = JSONObject(
            """
            {
              "from": "alice",
              "message_id": "msg-stale"
            }
            """.trimIndent()
        )

        val result = PrivateAckScope.ackableMessageId(
            payload = payload,
            chatId = "alice",
            currentlyActiveChatId = "bob",
        )

        assertNull(result)
    }
}
