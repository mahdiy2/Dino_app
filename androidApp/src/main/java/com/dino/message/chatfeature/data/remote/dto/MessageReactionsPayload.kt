package com.dino.message.chatfeature.data.remote.dto

import com.dino.message.chatfeature.domain.model.MessageReaction
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MessageReactionsPayload(
    @SerialName("message_id") val messageId: String,
    @SerialName("chat_id") val chatId: String? = null,
    @SerialName("group_id") val groupId: Int? = null,
    val scope: String? = null,
    val reactions: List<MessageReaction> = emptyList(),
)
