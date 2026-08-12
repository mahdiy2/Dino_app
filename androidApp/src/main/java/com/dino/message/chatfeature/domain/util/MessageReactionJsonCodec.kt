package com.dino.message.chatfeature.domain.util

import com.dino.message.chatfeature.domain.model.MessageReaction
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import org.json.JSONArray
import org.json.JSONObject

private val reactionJson = Json {
    ignoreUnknownKeys = true
    explicitNulls = false
}

fun encodeMessageReactions(reactions: List<MessageReaction>): String? {
    if (reactions.isEmpty()) return null
    return runCatching {
        reactionJson.encodeToString(ListSerializer(MessageReaction.serializer()), reactions)
    }.getOrNull()
}

fun decodeMessageReactions(reactionsJson: String?): List<MessageReaction> {
    if (reactionsJson.isNullOrBlank()) return emptyList()
    return runCatching {
        reactionJson.decodeFromString(
            ListSerializer(MessageReaction.serializer()),
            reactionsJson
        )
    }.getOrDefault(emptyList())
}

fun parseMessageReactionsJsonArray(reactionsArray: JSONArray?): List<MessageReaction> {
    if (reactionsArray == null) return emptyList()
    val reactions = mutableListOf<MessageReaction>()
    for (index in 0 until reactionsArray.length()) {
        val item = reactionsArray.optJSONObject(index) ?: continue
        val username = item.optString("username", "").trim()
        val emoji = item.optString("emoji", "").trim()
        if (username.isBlank() || emoji.isBlank()) continue
        reactions.add(
            MessageReaction(
                username = username,
                emoji = emoji,
                updatedAt = item.optString("updated_at", "").ifBlank { null }
            )
        )
    }
    return reactions
}

fun parseMessageReactionsFromPayload(payload: JSONObject): List<MessageReaction> {
    return parseMessageReactionsJsonArray(payload.optJSONArray("reactions"))
}
