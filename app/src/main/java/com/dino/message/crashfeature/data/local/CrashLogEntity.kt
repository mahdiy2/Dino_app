package com.dino.message.crashfeature.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "crash_logs_queue",
    indices = [
        Index(value = ["eventId"], unique = true),
        Index(value = ["createdAtEpochMillis"]),
    ],
)
data class CrashLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val eventId: String,
    val appVersion: String,
    val appVersionCode: Int,
    val threadName: String,
    val exceptionType: String,
    val exceptionMessage: String?,
    val stackTrace: String,
    val occurredAtEpochMillis: Long,
    val createdAtEpochMillis: Long,
    val deviceModel: String,
    val deviceManufacturer: String,
    val osVersion: String,
    val sdkInt: Int,
    val buildType: String,
    val username: String?,
    val sendAttempts: Int = 0,
    val lastError: String? = null,
)
