package com.dino.message.chatfeature.presentation.viewmodel

import org.json.JSONObject

internal object PrivateAckScope {
    fun ackableMessageId(
        payload: JSONObject,
        chatId: String,
        currentlyActiveChatId: String?,
    ): String? {
        if (chatId.isBlank()) return null
        if (currentlyActiveChatId != chatId) return null

        val sender = payload.optString("from", "").trim()
        if (sender != chatId) return null

        return payload.optString("message_id", "").trim().ifBlank { null }
    }
}
