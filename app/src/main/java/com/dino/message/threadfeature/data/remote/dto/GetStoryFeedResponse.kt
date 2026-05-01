package com.dino.message.threadfeature.data.remote.dto

import com.dino.message.corefeature.data.remote.ResponseToResultMapper
import com.dino.message.threadfeature.domain.model.StoryFeed
import com.dino.message.threadfeature.domain.model.StoryUserFeedItem
import kotlinx.serialization.Serializable

@Serializable
data class GetStoryFeedResponse(
    val user_stories: List<StoryUserFeedItem>,
    val generated_at: String? = null
) : ResponseToResultMapper<StoryFeed> {
    override fun mapResponseToResult(): StoryFeed = StoryFeed(
        user_stories = user_stories,
        generated_at = generated_at
    )
}
