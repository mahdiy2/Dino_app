package com.dino.message.corefeature.data.socket

import org.json.JSONObject

internal object SocketRetryPolicy {
    private const val PRIVATE_SEND_EVENT = "send_message"
    private const val GROUP_SEND_EVENT = "send_group_message"
    private const val UNKNOWN_SCOPE = "unknown"
    private val retryRequiredEvents = setOf(
        PRIVATE_SEND_EVENT,
        GROUP_SEND_EVENT,
    )

    fun requiresRetry(event: String): Boolean {
        return retryRequiredEvents.contains(event)
    }

    fun autoDedupeKey(event: String, payload: Any?): String? {
        if (!requiresRetry(event)) return null
        val jsonPayload = payload as? JSONObject ?: return null
        val clientMessageId = jsonPayload.optString("client_message_id", "").trim()
        if (clientMessageId.isBlank()) return null

        return when (event) {
            PRIVATE_SEND_EVENT -> {
                val recipient = jsonPayload.optString("to", "").trim().ifBlank { UNKNOWN_SCOPE }
                "private:$recipient:$clientMessageId"
            }
            GROUP_SEND_EVENT -> {
                val groupId = jsonPayload.optInt("group_id", -1)
                val scope = if (groupId > 0) groupId.toString() else UNKNOWN_SCOPE
                "group:$scope:$clientMessageId"
            }
            else -> null
        }
    }
}
