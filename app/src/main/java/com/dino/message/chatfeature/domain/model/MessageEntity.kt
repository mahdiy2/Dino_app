package com.dino.message.chatfeature.domain.model

 import androidx.room.Entity
 import androidx.room.Index
 import androidx.room.PrimaryKey


 @Entity(
     tableName = "messages",
     indices = [
         Index(name = "index_messages_messageId", value = ["messageId"], unique = true),
         Index(name = "index_messages_chatId", value = ["chatId"]),
         Index(name = "index_messages_chatId_timestamp_id", value = ["chatId", "timestamp", "id"]),
     ]
 )
 data class MessageEntity(
     @PrimaryKey(autoGenerate = true) val id: Int = 0,
     val messageId: String? = null,
     val chatId: String,
     val senderOrReceiver: String,
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
