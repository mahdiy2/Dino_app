package com.dino.message.corefeature.data.socket

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface SocketOutboundQueueDao {

    @Query(
        """
        SELECT * FROM socket_outbound_queue
        ORDER BY nextAttemptAtMs ASC, createdAtMs ASC
        """
    )
    suspend fun getAll(): List<SocketOutboundQueueEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(item: SocketOutboundQueueEntity): Long

    @Query("DELETE FROM socket_outbound_queue WHERE queueId = :queueId")
    suspend fun deleteByQueueId(queueId: String)

    @Query(
        """
        UPDATE socket_outbound_queue
        SET attempts = :attempts, nextAttemptAtMs = :nextAttemptAtMs
        WHERE queueId = :queueId
        """
    )
    suspend fun updateRetryState(
        queueId: String,
        attempts: Int,
        nextAttemptAtMs: Long,
    )

    @Query("DELETE FROM socket_outbound_queue")
    suspend fun clearAll()
}
