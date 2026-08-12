package com.dino.message.threadfeature.data.remote.dto

import com.dino.message.corefeature.data.remote.ResponseToResultMapper
import com.dino.message.threadfeature.domain.model.StoryViewResult
import kotlinx.serialization.Serializable

@Serializable
data class StoryViewResponse(
    val story_id: Long,
    val recorded: Boolean,
    val view_count: Int,
    val created: Boolean,
    val queued: Boolean
) : ResponseToResultMapper<StoryViewResult> {
    override fun mapResponseToResult(): StoryViewResult = StoryViewResult(
        story_id = story_id,
        recorded = recorded,
        view_count = view_count,
        created = created,
        queued = queued
    )
}
