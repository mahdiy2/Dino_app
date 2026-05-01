package com.dino.message.threadfeature.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Post(
    val id: Long,
    val text: String,
    val author: Author,
    val created_at: String,
    @SerialName("viewer_vote")
    val viewerVote: Long = 0L,
    val media: List<PostMedia>,
    @SerialName("quoted_post_id")
    val quotedPostId: Long? = null,
    @SerialName("quoted_post")
    val quotedPost: QuotedPostPreview? = null,
    @SerialName("is_daily_winner")
    val isDailyWinner: Boolean = false,
    @SerialName("was_daily_winner")
    val wasDailyWinner: Boolean = false,
    @SerialName("daily_winner_at")
    val dailyWinnerAt: String? = null
)
