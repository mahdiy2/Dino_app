package com.dino.message.chatfeature.presentation.viewmodel

import com.dino.message.chatfeature.domain.model.PrivateHistoryMessage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HistoryPendingCorrelationResolverTest {

    @Test
    fun resolve_returnsOutgoingMessagesWithClientCorrelation() {
        val result = HistoryPendingCorrelationResolver.resolve(
            historyMessages = listOf(
                historyMessage(
                    messageId = "server-1",
                    clientMessageId = "client-1",
                    from = "Me",
                ),
                historyMessage(
                    messageId = "server-2",
                    clientMessageId = "client-2",
                    from = "friend",
                ),
            ),
            currentUsername = "me",
        )

        assertEquals(1, result.size)
        assertEquals("client-1", result.first().clientMessageId)
        assertEquals("server-1", result.first().messageId)
    }

    @Test
    fun resolve_ignoresMessagesMissingIds() {
        val result = HistoryPendingCorrelationResolver.resolve(
            historyMessages = listOf(
                historyMessage(
                    messageId = "",
                    clientMessageId = "client-1",
                    from = "me",
                ),
                historyMessage(
                    messageId = "server-2",
                    clientMessageId = "",
                    from = "me",
                ),
            ),
            currentUsername = "me",
        )

        assertTrue(result.isEmpty())
    }

    private fun historyMessage(
        messageId: String,
        clientMessageId: String,
        from: String,
    ): PrivateHistoryMessage {
        return PrivateHistoryMessage(
            messageId = messageId,
            clientMessageId = clientMessageId,
            from = from,
            type = "text",
            message = "enc",
            encryptedKey = "key",
            attachment = null,
            timestamp = "2026-01-01T00:00:00.000000Z",
            replyToMessageId = null,
            replyToSender = null,
            encryptedReplyPreview = null,
            encryptedReplyKey = null,
        )
    }
}
