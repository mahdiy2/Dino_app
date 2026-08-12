package com.dino.message.groupfeature.data.remote.dto

import com.dino.message.chatfeature.data.remote.dto.MessageAttachment
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GroupHistoryResponseMapperTest {

    @Test
    fun mapResponseToResult_mapsMessagesAndCursor() {
        val response = GroupHistoryResponse(
            groupId = 42,
            hasMore = true,
            nextBefore = "2026-01-01T00:00:00.000000Z",
            messages = listOf(
                GroupHistoryMessageDto(
                    messageId = "g-1",
                    from = "alice",
                    type = "image",
                    message = "enc",
                    encryptedKeys = mapOf("alice" to "key-a", "bob" to "key-b"),
                    attachment = MessageAttachment(
                        type = "image",
                        url = "https://cdn.example.com/g.jpg",
                        mimeType = "image/jpeg",
                    ),
                    timestamp = "2026-01-01T00:01:00.000000Z",
                    replyToMessageId = "g-0",
                    replyToSender = "bob",
                    encryptedReplyPreview = "enc-preview",
                )
            ),
        )

        val mapped = response.mapResponseToResult()

        assertEquals(42, mapped.groupId)
        assertTrue(mapped.hasMore)
        assertEquals("2026-01-01T00:00:00.000000Z", mapped.nextBefore)
        assertEquals(1, mapped.messages.size)
        assertEquals("g-1", mapped.messages.first().messageId)
        assertEquals("key-a", mapped.messages.first().encryptedKeys?.get("alice"))
        assertEquals("image/jpeg", mapped.messages.first().attachment?.mimeType)
    }
}
