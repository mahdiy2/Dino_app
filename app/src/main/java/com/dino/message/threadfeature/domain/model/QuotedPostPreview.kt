package com.dino.message.threadfeature.domain.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

@Serializable
data class QuotedPostPreview(
    val id: Long,
    val text: String,
    val author: Author,
    val created_at: String,
    val media: List<PostMedia> = emptyList(),
    @SerialName("is_daily_winner")
    val isDailyWinner: Boolean = false,
    @SerialName("was_daily_winner")
    val wasDailyWinner: Boolean = false,
    @SerialName("daily_winner_at")
    val dailyWinnerAt: String? = null
)
