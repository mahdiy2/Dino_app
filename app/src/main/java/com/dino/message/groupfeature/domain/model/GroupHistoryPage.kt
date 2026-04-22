package com.dino.message.groupfeature.domain.model

import com.dino.message.chatfeature.data.remote.dto.MessageAttachment

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
    val encryptedKeys: Map<String, String>?,
    val encryptedKey: String?,
    val attachment: MessageAttachment?,
    val timestamp: String,
    val replyToMessageId: String?,
    val replyToSender: String?,
    val encryptedReplyPreview: String?,
)
