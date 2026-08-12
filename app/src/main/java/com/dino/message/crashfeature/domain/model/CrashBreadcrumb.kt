package com.dino.message.crashfeature.domain.model

data class CrashBreadcrumb(
    val timestampEpochMillis: Long,
    val category: String,
    val message: String,
)
