package com.dino.message.threadfeature.data.remote.dto

import com.dino.message.corefeature.data.remote.ResponseToResultMapper
import com.dino.message.threadfeature.domain.model.StoryBundle
import com.dino.message.threadfeature.domain.model.StoryDetail
import com.dino.message.threadfeature.domain.model.StoryOwner
import kotlinx.serialization.Serializable

@Serializable
data class GetStoryBundleResponse(
    val story: StoryDetail,
    val owner: StoryOwner,
    val user_stories: List<StoryDetail>
) : ResponseToResultMapper<StoryBundle> {
    override fun mapResponseToResult(): StoryBundle = StoryBundle(
        story = story,
        owner = owner,
        user_stories = user_stories
    )
}
