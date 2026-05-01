package com.dino.message.threadfeature.data.remote.dto

import com.dino.message.corefeature.data.remote.ResponseToResultMapper
import com.dino.message.threadfeature.domain.model.StoryViewer
import com.dino.message.threadfeature.domain.model.StoryViewersPage
import kotlinx.serialization.Serializable

@Serializable
data class GetStoryViewersResponse(
    val story_id: Long,
    val page: Int,
    val limit: Int,
    val total: Int,
    val viewers: List<StoryViewer>
) : ResponseToResultMapper<StoryViewersPage> {
    override fun mapResponseToResult(): StoryViewersPage = StoryViewersPage(
        story_id = story_id,
        page = page,
        limit = limit,
        total = total,
        viewers = viewers
    )
}
