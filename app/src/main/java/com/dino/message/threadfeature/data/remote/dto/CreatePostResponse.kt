package com.dino.message.threadfeature.data.remote.dto

import com.dino.message.corefeature.data.remote.ResponseToResultMapper
import com.dino.message.corefeature.data.remote.dto.BaseResponse
import com.dino.message.threadfeature.domain.model.CreatedPost
import kotlinx.serialization.Serializable

@Serializable
data class CreatePostResponse(
    val post_id: Long,
    override val message: String
) : BaseResponse(), ResponseToResultMapper<CreatedPost> {
    override fun mapResponseToResult(): CreatedPost = CreatedPost(
        post_id = post_id,
        message = message
    )
}
