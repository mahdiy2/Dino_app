package com.dino.message.threadfeature.domain.model

data class FeedPage(
    val mode: FeedMode,
    val limit: Int,
    val hasMore: Boolean,
    val nextCursor: String?,
    val posts: List<Post>,
)
