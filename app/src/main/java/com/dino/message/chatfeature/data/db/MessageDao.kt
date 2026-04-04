package com.dino.message.chatfeature.data.db

import androidx.room.*
import com.dino.message.chatfeature.domain.model.MessageEntity

@Dao
interface MessageDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertMessage(message: MessageEntity)

    @Query(
        """
        SELECT * FROM messages
        WHERE chatId = :chatId
        ORDER BY
            CASE WHEN timestamp IS NULL OR timestamp = '' THEN 1 ELSE 0 END,
            timestamp DESC,
            id DESC
        """
    )
    suspend fun getMessagesByChatId(chatId: String): List<MessageEntity>

    @Query("DELETE FROM messages WHERE chatId = :chatId")
    suspend fun deleteMessagesByChatId(chatId: String)

    @Query("DELETE FROM messages WHERE id = :localId")
    suspend fun deleteMessageByLocalId(localId: Int)

    @Query("DELETE FROM messages WHERE messageId = :messageId")
    suspend fun deleteMessageByMessageId(messageId: String)

    @Query("UPDATE messages SET isSeen = 1 WHERE messageId = :messageId")
    suspend fun markMessageSeenByMessageId(messageId: String)

    @Query("SELECT * FROM messages ORDER BY timestamp DESC")
    suspend fun getAllMessages(): List<MessageEntity>

    @Query("DELETE FROM messages")
    suspend fun deleteAllMessages()
}
