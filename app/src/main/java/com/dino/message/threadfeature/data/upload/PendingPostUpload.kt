package com.dino.message.threadfeature.data.upload

import java.util.UUID

data class PendingPostUpload(
    val workId: UUID,
    val text: String,
    val mediaCount: Int,
    val progressPercent: Int,
    val isRunning: Boolean,
    val createdAtEpochMs: Long,
)
