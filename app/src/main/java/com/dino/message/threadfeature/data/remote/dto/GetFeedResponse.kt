package com.dino.message.threadfeature.data.remote.dto

import com.dino.message.corefeature.data.remote.ResponseToResultMapper
import com.dino.message.threadfeature.domain.model.FeedMode
import com.dino.message.threadfeature.domain.model.FeedPage
import com.dino.message.threadfeature.domain.model.Post
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GetFeedResponse(
    val mode: String = FeedMode.MIXED.apiValue,
    val limit: Int,
    @SerialName("has_more")
    val hasMore: Boolean,
    @SerialName("next_cursor")
    val nextCursor: String? = null,
    val posts: List<Post>,
) : ResponseToResultMapper<FeedPage> {
    override fun mapResponseToResult(): FeedPage = FeedPage(
        mode = mode.toFeedMode(),
        limit = limit,
        hasMore = hasMore,
        nextCursor = nextCursor,
        posts = posts,
    )
}

private fun String.toFeedMode(): FeedMode = when (trim().lowercase()) {
    FeedMode.MUSIC.apiValue -> FeedMode.MUSIC
    FeedMode.FOLLOWERS.apiValue -> FeedMode.FOLLOWERS
    else -> FeedMode.MIXED
}
