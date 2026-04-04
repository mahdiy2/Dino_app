package com.dino.message.groupfeature.data.db

import androidx.room.*
import com.dino.message.groupfeature.domain.model.GroupMessageEntity

@Dao
interface GroupMessageDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertMessage(message: GroupMessageEntity)

    @Query("SELECT * FROM group_messages WHERE groupId = :groupId ORDER BY timestamp DESC")
    suspend fun getMessagesByGroupId(groupId: Int): List<GroupMessageEntity>

    @Query("DELETE FROM group_messages WHERE groupId = :groupId")
    suspend fun deleteMessagesByGroupId(groupId: Int)

    @Query("DELETE FROM group_messages WHERE messageId = :messageId")
    suspend fun deleteMessageByMessageId(messageId: String)

    @Query("UPDATE group_messages SET isSeen = 1 WHERE messageId = :messageId")
    suspend fun markMessageSeenByMessageId(messageId: String)
}
