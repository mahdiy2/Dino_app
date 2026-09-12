package com.dino.message.groupfeature.domain.model

import com.dino.message.chatfeature.domain.model.MessageAttachment
import com.dino.message.chatfeature.domain.model.MessageReaction

data class GroupHistoryPage(
    val groupId: Int,
    val messages: List<GroupHistoryMessage>,
    val hasMore: Boolean,
    val nextBefore: String?,
)

data class GroupHistoryMessage(
    val messageId: String,
    val from: String,
    val type: String,
    val message: String?,
    val encryptionMode: String? = null,
    val encryptedKeys: Map<String, String>?,
    val encryptedKey: String?,
    val expiresAt: String? = null,
    val attachment: MessageAttachment?,
    val timestamp: String,
    val replyToMessageId: String?,
    val replyToSender: String?,
    val encryptedReplyPreview: String?,
    val reactions: List<MessageReaction> = emptyList(),
)
