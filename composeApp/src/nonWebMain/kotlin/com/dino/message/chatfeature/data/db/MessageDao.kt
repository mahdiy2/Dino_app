package com.dino.message.chatfeature.data.db

import androidx.room.*
import com.dino.message.chatfeature.data.local.MessageLocalEntity

private const val SENT_MESSAGE_PLACEHOLDER = "Sent message"

@Dao
interface MessageDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertMessage(message: MessageLocalEntity)

    @Query(
        """
        SELECT * FROM messages
        WHERE chatId = :chatId
        ORDER BY
            CASE WHEN timestamp IS NULL OR timestamp = '' THEN 1 ELSE 0 END,
            timestamp DESC,
            id DESC
        LIMIT :limit OFFSET :offset
        """
    )
    suspend fun getMessagesByChatId(chatId: String, limit: Int, offset: Int): List<MessageLocalEntity>

    @Query("SELECT COUNT(*) FROM messages WHERE chatId = :chatId")
    suspend fun getMessageCountByChatId(chatId: String): Int

    @Query("DELETE FROM messages WHERE chatId = :chatId")
    suspend fun deleteMessagesByChatId(chatId: String)

    @Query("DELETE FROM messages WHERE id = :localId")
    suspend fun deleteMessageByLocalId(localId: Int)

    @Query("DELETE FROM messages WHERE messageId = :messageId")
    suspend fun deleteMessageByMessageId(messageId: String)

    @Query("UPDATE messages SET isSeen = 1 WHERE messageId = :messageId")
    suspend fun markMessageSeenByMessageId(messageId: String)

    @Query("UPDATE messages SET reactionsJson = :reactionsJson WHERE messageId = :messageId")
    suspend fun updateReactionsByMessageId(messageId: String, reactionsJson: String?)

    @Query("SELECT * FROM messages ORDER BY timestamp DESC")
    suspend fun getAllMessages(): List<MessageLocalEntity>

    @Query("DELETE FROM messages")
    suspend fun deleteAllMessages()

    @Query("SELECT * FROM messages WHERE messageId = :messageId LIMIT 1")
    suspend fun getMessageByMessageId(messageId: String): MessageLocalEntity?

    @Update
    suspend fun updateMessage(message: MessageLocalEntity)

    @Transaction
    suspend fun upsertMessagesByMessageId(messages: List<MessageLocalEntity>) {
        messages.forEach { message ->
            val messageId = message.messageId
            if (messageId.isNullOrBlank()) {
                insertMessage(message)
                return@forEach
            }

            val existing = getMessageByMessageId(messageId)
            if (existing == null) {
                insertMessage(message)
            } else {
                updateMessage(
                    message.copy(
                        id = existing.id,
                        content = if (
                            message.content.isBlank() ||
                            (message.content == SENT_MESSAGE_PLACEHOLDER && existing.content.isNotBlank())
                        ) {
                            existing.content
                        } else {
                            message.content
                        },
                        timestamp = if (message.timestamp.isBlank()) existing.timestamp else message.timestamp,
                        attachmentType = message.attachmentType ?: existing.attachmentType,
                        attachmentUrl = message.attachmentUrl ?: existing.attachmentUrl,
                        attachmentMimeType = message.attachmentMimeType ?: existing.attachmentMimeType,
                        attachmentFileName = message.attachmentFileName ?: existing.attachmentFileName,
                        attachmentSizeBytes = message.attachmentSizeBytes ?: existing.attachmentSizeBytes,
                        isSeen = existing.isSeen || message.isSeen,
                        replyToMessageId = message.replyToMessageId ?: existing.replyToMessageId,
                        replyToSender = message.replyToSender ?: existing.replyToSender,
                        replyPreview = message.replyPreview ?: existing.replyPreview,
                        reactionsJson = message.reactionsJson ?: existing.reactionsJson,
                    )
                )
            }
        }
    }
}
