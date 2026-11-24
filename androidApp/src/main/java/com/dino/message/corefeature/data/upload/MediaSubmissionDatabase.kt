package com.dino.message.corefeature.data.upload

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "media_submissions")
data class MediaSubmissionRow(
    @PrimaryKey val id: String,
    val owner: String,
    val sessionId: String,
    val destination: String,
    val command: String,
    val createdAt: Long,
    val state: String = "pending",
    val result: String? = null,
    val failure: String? = null,
)

@Dao
interface MediaSubmissionDao {
    @Insert
    suspend fun insert(row: MediaSubmissionRow)

    @Query("SELECT * FROM media_submissions WHERE id = :id")
    suspend fun get(id: String): MediaSubmissionRow?

    @Query("SELECT * FROM media_submissions WHERE id = :id")
    fun observe(id: String): Flow<MediaSubmissionRow?>

    @Query("SELECT * FROM media_submissions WHERE owner = :owner ORDER BY createdAt DESC")
    fun observeOwner(owner: String): Flow<List<MediaSubmissionRow>>

    @Query("SELECT * FROM media_submissions")
    suspend fun all(): List<MediaSubmissionRow>

    @Query("UPDATE media_submissions SET result = :result WHERE id = :id AND state = 'pending'")
    suspend fun saveResult(id: String, result: String): Int

    @Query("UPDATE media_submissions SET state = :state, failure = :failure WHERE id = :id AND state = 'pending'")
    suspend fun finish(id: String, state: String, failure: String? = null): Int

    @Query("UPDATE media_submissions SET state = 'pending', failure = NULL WHERE id = :id AND state = 'retryable'")
    suspend fun reopenFailed(id: String): Int

    @Query("DELETE FROM media_submissions WHERE id = :id")
    suspend fun delete(id: String)
}

@Database(entities = [MediaSubmissionRow::class], version = 1, exportSchema = false)
abstract class MediaSubmissionDatabase : RoomDatabase() {
    abstract fun submissions(): MediaSubmissionDao
}
