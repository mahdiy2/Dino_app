package com.dino.message.mainpage.presentation.util

import com.dino.message.mainpage.domain.model.MessageList

data class ContactsDeltaApplyResult(
    val contacts: List<MessageList?>,
    val insertedNewContact: Boolean,
)

object ContactsDeltaReducer {

    fun apply(
        currentContacts: List<MessageList?>,
        contactUsername: String,
        hasUnread: Boolean,
        messageTimestamp: String,
        messageType: String?,
    ): ContactsDeltaApplyResult {
        val currentItems = currentContacts.toMutableList()
        val existingIndex = currentItems.indexOfFirst { it?.username == contactUsername }
        val existingItem = if (existingIndex >= 0) currentItems.removeAt(existingIndex) else null

        val mergedItem = if (existingItem != null) {
            existingItem.copy(
                time = messageTimestamp.ifBlank { existingItem.time },
                newMessage = hasUnread,
                lastMessageType = messageType ?: existingItem.lastMessageType,
            )
        } else {
            MessageList(
                username = contactUsername,
                displayName = contactUsername,
                time = messageTimestamp,
                newMessage = hasUnread,
                lastMessageType = messageType,
            )
        }

        currentItems.add(0, mergedItem)
        return ContactsDeltaApplyResult(
            contacts = currentItems,
            insertedNewContact = existingItem == null,
        )
    }
}
