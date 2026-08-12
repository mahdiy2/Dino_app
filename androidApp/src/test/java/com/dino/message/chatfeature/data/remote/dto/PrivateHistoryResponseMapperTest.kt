package com.dino.message.chatfeature.data.remote.dto

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PrivateHistoryResponseMapperTest {

    @Test
    fun mapResponseToResult_mapsMessagesAndCursor() {
        val response = PrivateHistoryResponse(
            chatId = "alice",
            hasMore = true,
            nextBefore = "2026-01-01T00:00:00.000000Z",
            messages = listOf(
                PrivateHistoryMessageDto(
                    messageId = "m-1",
                    clientMessageId = "client-1",
                    from = "alice",
                    type = "text",
                    message = "enc",
                    encryptedKey = "key",
                    attachment = MessageAttachment(
                        type = "image",
                        url = "https://cdn.example.com/a.jpg",
                        mimeType = "image/jpeg",
                    ),
                    timestamp = "2026-01-01T00:01:00.000000Z",
                    replyToMessageId = "m-0",
                    replyToSender = "bob",
                    encryptedReplyPreview = "enc-preview",
                    encryptedReplyKey = "reply-key",
                )
            ),
        )

        val mapped = response.mapResponseToResult()

        assertEquals("alice", mapped.chatId)
        assertTrue(mapped.hasMore)
        assertEquals("2026-01-01T00:00:00.000000Z", mapped.nextBefore)
        assertEquals(1, mapped.messages.size)
        assertEquals("m-1", mapped.messages.first().messageId)
        assertEquals("client-1", mapped.messages.first().clientMessageId)
        assertEquals("enc", mapped.messages.first().message)
        assertEquals("reply-key", mapped.messages.first().encryptedReplyKey)
        assertEquals("image/jpeg", mapped.messages.first().attachment?.mimeType)
    }
}
