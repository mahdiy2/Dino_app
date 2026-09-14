package com.dino.message.crashfeature.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface CrashLogDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(entity: CrashLogEntity): Long

    @Query(
        """
        SELECT * FROM crash_logs_queue
        ORDER BY createdAtEpochMillis ASC
        LIMIT :limit
        """
    )
    suspend fun getOldestPending(limit: Int): List<CrashLogEntity>

    @Query("DELETE FROM crash_logs_queue WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>)

    @Query(
        """
        UPDATE crash_logs_queue
        SET sendAttempts = sendAttempts + 1,
            lastError = :error
        WHERE id IN (:ids)
        """
    )
    suspend fun markFailed(ids: List<Long>, error: String?)
}
