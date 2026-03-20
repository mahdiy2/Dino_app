package com.dino.message.chatfeature.data.db

import androidx.room.*
import com.dino.message.chatfeature.domain.model.MessageEntity

@Dao
interface MessageDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertMessage(message: MessageEntity)

    @Query("SELECT * FROM messages WHERE chatId = :chatId ORDER BY timestamp DESC")
    suspend fun getMessagesByChatId(chatId: String): List<MessageEntity>

    @Query("DELETE FROM messages WHERE chatId = :chatId")
    suspend fun deleteMessagesByChatId(chatId: String)

    @Query("SELECT * FROM messages ORDER BY timestamp DESC")
    suspend fun getAllMessages(): List<MessageEntity>

    @Query("DELETE FROM messages")
    suspend fun deleteAllMessages()
}
