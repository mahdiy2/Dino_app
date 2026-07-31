package com.dino.message.groupfeature.data.db

import androidx.room.*
import com.dino.message.groupfeature.data.local.GroupMessageLocalEntity

@Dao
interface GroupMessageDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertMessage(message: GroupMessageLocalEntity)

    @Query(
        """
        SELECT * FROM group_messages
        WHERE groupId = :groupId
        ORDER BY
            CASE WHEN timestamp IS NULL OR timestamp = '' THEN 1 ELSE 0 END,
            timestamp DESC,
            id DESC
        LIMIT :limit OFFSET :offset
        """
    )
    suspend fun getMessagesByGroupId(groupId: Int, limit: Int, offset: Int): List<GroupMessageLocalEntity>

    @Query("SELECT COUNT(*) FROM group_messages WHERE groupId = :groupId")
    suspend fun getMessageCountByGroupId(groupId: Int): Int

    @Query("DELETE FROM group_messages WHERE groupId = :groupId")
    suspend fun deleteMessagesByGroupId(groupId: Int)

    @Query("DELETE FROM group_messages WHERE messageId = :messageId")
    suspend fun deleteMessageByMessageId(messageId: String)

    @Query("UPDATE group_messages SET isSeen = 1 WHERE messageId = :messageId")
    suspend fun markMessageSeenByMessageId(messageId: String)

    @Query("UPDATE group_messages SET reactionsJson = :reactionsJson WHERE messageId = :messageId")
    suspend fun updateReactionsByMessageId(messageId: String, reactionsJson: String?)

    @Query("SELECT * FROM group_messages WHERE messageId = :messageId LIMIT 1")
    suspend fun getMessageByMessageId(messageId: String): GroupMessageLocalEntity?

    @Update
    suspend fun updateMessage(message: GroupMessageLocalEntity)

    @Transaction
    suspend fun upsertMessagesByMessageId(messages: List<GroupMessageLocalEntity>) {
        messages.forEach { message ->
            val existing = getMessageByMessageId(message.messageId)
            if (existing == null) {
                insertMessage(message)
            } else {
                updateMessage(
                    message.copy(
                        id = existing.id,
                        content = if (message.content.isBlank()) existing.content else message.content,
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
