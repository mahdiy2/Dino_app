package com.dino.message.threadfeature.data.remote.dto

import com.dino.message.corefeature.data.remote.ResponseToResultMapper
import com.dino.message.threadfeature.domain.model.StoryReplyResult
import kotlinx.serialization.Serializable

@Serializable
data class StoryReplyResponse(
    val story_id: Long,
    val recipient: String,
    val message: String
) : ResponseToResultMapper<StoryReplyResult> {
    override fun mapResponseToResult(): StoryReplyResult = StoryReplyResult(
        story_id = story_id,
        recipient = recipient,
        message = message
    )
}
