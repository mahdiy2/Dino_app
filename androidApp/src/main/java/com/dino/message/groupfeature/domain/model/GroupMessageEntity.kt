package com.dino.message.groupfeature.domain.model

data class GroupMessageEntity(
    val id: Int = 0,
    val messageId: String,
    val groupId: Int,
    val sender: String,
    val content: String,
    val timestamp: String,
    val messageType: String = "text",
    val attachmentType: String? = null,
    val attachmentUrl: String? = null,
    val attachmentMimeType: String? = null,
    val attachmentFileName: String? = null,
    val attachmentSizeBytes: Long? = null,
    val isSeen: Boolean = false,
    val replyToMessageId: String? = null,
    val replyToSender: String? = null,
    val replyPreview: String? = null,
    val reactionsJson: String? = null,
)
