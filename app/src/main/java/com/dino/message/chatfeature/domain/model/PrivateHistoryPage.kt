package com.dino.message.chatfeature.domain.model

import com.dino.message.chatfeature.data.remote.dto.MessageAttachment

data class PrivateHistoryPage(
    val chatId: String,
    val messages: List<PrivateHistoryMessage>,
    val hasMore: Boolean,
    val nextBefore: String?,
)

data class PrivateHistoryMessage(
    val messageId: String?,
    val clientMessageId: String?,
    val from: String,
    val type: String,
    val message: String?,
    val encryptedKey: String?,
    val attachment: MessageAttachment?,
    val timestamp: String,
    val replyToMessageId: String?,
    val replyToSender: String?,
    val encryptedReplyPreview: String?,
    val encryptedReplyKey: String?,
)
