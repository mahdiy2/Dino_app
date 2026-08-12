package com.dino.message.groupfeature.data.remote.dto

import com.dino.message.chatfeature.data.remote.dto.MessageAttachment
import com.dino.message.chatfeature.data.remote.dto.toDomain
import com.dino.message.chatfeature.domain.model.MessageReaction
import com.dino.message.corefeature.data.remote.ResponseToResultMapper
import com.dino.message.groupfeature.domain.model.GroupHistoryMessage
import com.dino.message.groupfeature.domain.model.GroupHistoryPage
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GroupHistoryResponse(
    @SerialName("group_id") val groupId: Int,
    val messages: List<GroupHistoryMessageDto> = emptyList(),
    @SerialName("has_more") val hasMore: Boolean = false,
    @SerialName("next_before") val nextBefore: String? = null,
) : ResponseToResultMapper<GroupHistoryPage> {
    override fun mapResponseToResult(): GroupHistoryPage = GroupHistoryPage(
        groupId = groupId,
        messages = messages.map { it.toDomain() },
        hasMore = hasMore,
        nextBefore = nextBefore,
    )
}

@Serializable
data class GroupHistoryMessageDto(
    @SerialName("message_id") val messageId: String,
    val from: String,
    val type: String = "text",
    val message: String? = null,
    @SerialName("encryption_mode") val encryptionMode: String? = null,
    @SerialName("encrypted_keys") val encryptedKeys: Map<String, String>? = null,
    @SerialName("encrypted_key") val encryptedKey: String? = null,
    @SerialName("expires_at") val expiresAt: String? = null,
    val attachment: MessageAttachment? = null,
    val timestamp: String = "",
    @SerialName("reply_to_message_id") val replyToMessageId: String? = null,
    @SerialName("reply_to_sender") val replyToSender: String? = null,
    @SerialName("encrypted_reply_preview") val encryptedReplyPreview: String? = null,
    val reactions: List<MessageReaction> = emptyList(),
)

private fun GroupHistoryMessageDto.toDomain(): GroupHistoryMessage = GroupHistoryMessage(
    messageId = messageId,
    from = from,
    type = type,
    message = message,
    encryptionMode = encryptionMode,
    encryptedKeys = encryptedKeys,
    encryptedKey = encryptedKey,
    expiresAt = expiresAt,
    attachment = attachment?.toDomain(),
    timestamp = timestamp,
    replyToMessageId = replyToMessageId,
    replyToSender = replyToSender,
    encryptedReplyPreview = encryptedReplyPreview,
    reactions = reactions,
)
