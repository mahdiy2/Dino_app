package com.dino.message.corefeature.data.socket

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

public object SocketRetryPolicy {
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

    private fun JsonObject.string(key: String, default: String): String =
        (get(key) as? JsonPrimitive)?.takeUnless { it == kotlinx.serialization.json.JsonNull }?.content ?: default

    fun requiresRetry(event: String): Boolean {
        return retryRequiredEvents.contains(event)
    }

    fun autoDedupeKey(event: String, payload: JsonObject?): String? {
        if (!requiresRetry(event)) return null
        val jsonPayload = payload ?: return null
        val clientMessageId = jsonPayload.string("client_message_id", "").trim()
        if (clientMessageId.isBlank()) return null

        return when (event) {
            PRIVATE_SEND_EVENT -> {
                val recipient = jsonPayload.string("to", "").trim().ifBlank { UNKNOWN_SCOPE }
                "private:$recipient:$clientMessageId"
            }
            GROUP_SEND_EVENT -> {
                val groupId = jsonPayload.string("group_id", "").toDoubleOrNull()?.toInt() ?: -1
                val scope = if (groupId > 0) groupId.toString() else UNKNOWN_SCOPE
                "group:$scope:$clientMessageId"
            }
            PRIVATE_DELETE_EVENT,
            PRIVATE_DELETE_FOR_ME_EVENT -> {
                val counterpart = jsonPayload.string("chat_id", "").trim().ifBlank { UNKNOWN_SCOPE }
                val messageId = jsonPayload.string("message_id", "").trim().ifBlank { UNKNOWN_SCOPE }
                "private-delete:$event:$counterpart:$messageId"
            }
            GROUP_DELETE_EVENT,
            GROUP_DELETE_FOR_ME_EVENT -> {
                val groupId = jsonPayload.string("group_id", "").toDoubleOrNull()?.toInt() ?: -1
                val scope = if (groupId > 0) groupId.toString() else UNKNOWN_SCOPE
                val messageId = jsonPayload.string("message_id", "").trim().ifBlank { UNKNOWN_SCOPE }
                "group-delete:$event:$scope:$messageId"
            }
            else -> null
        }
    }
}
