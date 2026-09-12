package com.dino.message.mainpage.presentation.util

import com.dino.message.mainpage.domain.model.MessageList
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.Test

class ContactsDeltaReducerTest {

    @Test
    fun apply_updatesExistingContact_movesItToTop_andUsesDeltaValues() {
        val contacts = listOf(
            MessageList(
                username = "alice",
                displayName = "alice",
                time = "2026-04-20T10:00:00.000000Z",
                newMessage = false,
                lastMessageType = "text",
            ),
            MessageList(
                username = "bob",
                displayName = "bob",
                time = "2026-04-20T09:00:00.000000Z",
                newMessage = true,
                lastMessageType = "text",
            ),
        )

        val result = ContactsDeltaReducer.apply(
            currentContacts = contacts,
            contactUsername = "bob",
            hasUnread = false,
            messageTimestamp = "2026-04-21T08:00:00.000000Z",
            messageType = "image",
        )

        assertFalse(result.insertedNewContact)
        assertEquals("bob", result.contacts.first()?.username)
        assertFalse(result.contacts.first()?.newMessage ?: true)
        assertEquals("image", result.contacts.first()?.lastMessageType)
    }

    @Test
    fun apply_insertsMissingContact_atTopWithFallbackDisplayName() {
        val contacts = listOf<MessageList?>(
            null,
            MessageList(
                username = "alice",
                displayName = "alice",
                time = "",
                newMessage = false,
            ),
        )

        val result = ContactsDeltaReducer.apply(
            currentContacts = contacts,
            contactUsername = "bob",
            hasUnread = true,
            messageTimestamp = "2026-04-21T09:00:00.000000Z",
            messageType = "text",
        )

        assertTrue(result.insertedNewContact)
        assertEquals("bob", result.contacts.first()?.username)
        assertEquals("bob", result.contacts.first()?.displayName)
        assertTrue(result.contacts.first()?.newMessage ?: false)
    }

    @Test
    fun apply_keepsExistingTimestampWhenDeltaTimestampIsBlank() {
        val contacts = listOf(
            MessageList(
                username = "alice",
                displayName = "alice",
                time = "2026-04-20T10:00:00.000000Z",
                newMessage = false,
                lastMessageType = "audio",
            ),
        )

        val result = ContactsDeltaReducer.apply(
            currentContacts = contacts,
            contactUsername = "alice",
            hasUnread = true,
            messageTimestamp = "",
            messageType = null,
        )

        assertFalse(result.insertedNewContact)
        assertEquals("2026-04-20T10:00:00.000000Z", result.contacts.first()?.time)
        assertEquals("audio", result.contacts.first()?.lastMessageType)
        assertTrue(result.contacts.first()?.newMessage ?: false)
    }
}
