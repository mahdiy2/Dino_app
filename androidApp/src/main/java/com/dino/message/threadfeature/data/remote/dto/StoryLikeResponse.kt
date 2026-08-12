package com.dino.message.threadfeature.data.remote.dto

import com.dino.message.corefeature.data.remote.ResponseToResultMapper
import com.dino.message.threadfeature.domain.model.StoryLikeResult
import kotlinx.serialization.Serializable

@Serializable
data class StoryLikeResponse(
    val story_id: Long,
    val liked: Boolean,
    val like_count: Int,
    val view_count: Int
) : ResponseToResultMapper<StoryLikeResult> {
    override fun mapResponseToResult(): StoryLikeResult = StoryLikeResult(
        story_id = story_id,
        liked = liked,
        like_count = like_count,
        view_count = view_count
    )
}
