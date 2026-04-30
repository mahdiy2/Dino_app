package com.dino.message.corefeature.data.socket

import org.json.JSONObject

internal object SocketRetryPolicy {
    private const val PRIVATE_SEND_EVENT = "send_message"
    private const val GROUP_SEND_EVENT = "send_group_message"
    private const val PRIVATE_DELETE_EVENT = "delete_message"
    private const val PRIVATE_DELETE_FOR_ME_EVENT = "delete_message_for_me"
    private const val GROUP_DELETE_EVENT = "delete_group_message"
    private const val GROUP_DELETE_FOR_ME_EVENT = "delete_group_message_for_me"
    private const val UNKNOWN_SCOPE = "unknown"
    private val retryRequiredEvents = setOf(
        PRIVATE_SEND_EVENT,
        GROUP_SEND_EVENT,
        PRIVATE_DELETE_EVENT,
        PRIVATE_DELETE_FOR_ME_EVENT,
        GROUP_DELETE_EVENT,
        GROUP_DELETE_FOR_ME_EVENT,
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
            PRIVATE_DELETE_EVENT,
            PRIVATE_DELETE_FOR_ME_EVENT -> {
                val counterpart = jsonPayload.optString("chat_id", "").trim().ifBlank { UNKNOWN_SCOPE }
                val messageId = jsonPayload.optString("message_id", "").trim().ifBlank { UNKNOWN_SCOPE }
                "private-delete:$event:$counterpart:$messageId"
            }
            GROUP_DELETE_EVENT,
            GROUP_DELETE_FOR_ME_EVENT -> {
                val groupId = jsonPayload.optInt("group_id", -1)
                val scope = if (groupId > 0) groupId.toString() else UNKNOWN_SCOPE
                val messageId = jsonPayload.optString("message_id", "").trim().ifBlank { UNKNOWN_SCOPE }
                "group-delete:$event:$scope:$messageId"
            }
            else -> null
        }
    }
}
