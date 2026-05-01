package com.dino.message.threadfeature.data.remote.dto

import com.dino.message.corefeature.data.remote.ResponseToResultMapper
import com.dino.message.threadfeature.domain.model.StoryUploadResult
import kotlinx.serialization.Serializable

@Serializable
data class StoryUploadResponse(
    val story_id: Long,
    val media_url: String,
    val media_type: String,
    val expires_at: String? = null,
    val mention_user_ids: List<Long> = emptyList()
) : ResponseToResultMapper<StoryUploadResult> {
    override fun mapResponseToResult(): StoryUploadResult = StoryUploadResult(
        story_id = story_id,
        media_url = media_url,
        media_type = media_type,
        expires_at = expires_at,
        mention_user_ids = mention_user_ids
    )
}
