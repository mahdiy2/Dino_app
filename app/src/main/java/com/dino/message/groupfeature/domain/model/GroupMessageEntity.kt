package com.dino.message.groupfeature.domain.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "group_messages",
    indices = [
        Index(name = "index_group_messages_messageId", value = ["messageId"], unique = true),
        Index(name = "index_group_messages_groupId", value = ["groupId"]),
        Index(name = "index_group_messages_groupId_timestamp_id", value = ["groupId", "timestamp", "id"]),
    ]
)
data class GroupMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
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
)
