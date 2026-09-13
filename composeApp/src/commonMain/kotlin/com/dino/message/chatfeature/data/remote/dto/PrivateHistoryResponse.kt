package com.dino.message.chatfeature.data.remote.dto

import com.dino.message.chatfeature.domain.model.PrivateHistoryMessage
import com.dino.message.chatfeature.domain.model.PrivateHistoryPage
import com.dino.message.chatfeature.domain.model.MessageReaction
import com.dino.message.corefeature.data.remote.ResponseToResultMapper
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PrivateHistoryResponse(
    @SerialName("chat_id") val chatId: String,
    val messages: List<PrivateHistoryMessageDto> = emptyList(),
    @SerialName("has_more") val hasMore: Boolean = false,
    @SerialName("next_before") val nextBefore: String? = null,
) : ResponseToResultMapper<PrivateHistoryPage> {
    override fun mapResponseToResult(): PrivateHistoryPage = PrivateHistoryPage(
        chatId = chatId,
        messages = messages.map { it.toDomain() },
        hasMore = hasMore,
        nextBefore = nextBefore,
    )
}

@Serializable
data class PrivateHistoryMessageDto(
    @SerialName("message_id") val messageId: String? = null,
    @SerialName("client_message_id") val clientMessageId: String? = null,
    val from: String,
    val type: String = "text",
    val message: String? = null,
    @SerialName("encrypted_key") val encryptedKey: String? = null,
    val attachment: MessageAttachment? = null,
    val timestamp: String = "",
    @SerialName("reply_to_message_id") val replyToMessageId: String? = null,
    @SerialName("reply_to_sender") val replyToSender: String? = null,
    @SerialName("encrypted_reply_preview") val encryptedReplyPreview: String? = null,
    @SerialName("encrypted_reply_key") val encryptedReplyKey: String? = null,
    val reactions: List<MessageReaction> = emptyList(),
)

private fun PrivateHistoryMessageDto.toDomain(): PrivateHistoryMessage = PrivateHistoryMessage(
    messageId = messageId,
    clientMessageId = clientMessageId,
    from = from,
    type = type,
    message = message,
    encryptedKey = encryptedKey,
    attachment = attachment?.toDomain(),
    timestamp = timestamp,
    replyToMessageId = replyToMessageId,
    replyToSender = replyToSender,
    encryptedReplyPreview = encryptedReplyPreview,
    encryptedReplyKey = encryptedReplyKey,
    reactions = reactions,
)
