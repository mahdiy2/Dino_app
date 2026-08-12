package com.dino.message.mainpage.data.remote.dto

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ContactsDeltaResponseMapperTest {

    @Test
    fun mapResponseToResult_mapsContactsList() {
        val response = ContactsDeltaResponse(
            contacts = listOf(
                DetailedContact(
                    username = "alice",
                    name = "Alice",
                    hasMessage = true,
                    online = true,
                    lastMessage = ContactLastMessage(
                        from = "alice",
                        type = "text",
                        timestamp = "2026-04-21T12:00:00.000000Z",
                    ),
                )
            )
        )

        val mapped = response.mapResponseToResult()

        assertEquals(1, mapped.size)
        assertEquals("alice", mapped.first().username)
        assertTrue(mapped.first().hasMessage)
    }
}
