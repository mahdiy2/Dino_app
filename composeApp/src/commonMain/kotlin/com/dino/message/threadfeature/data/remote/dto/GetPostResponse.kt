package com.dino.message.threadfeature.data.remote.dto

import com.dino.message.corefeature.data.remote.ResponseToResultMapper
import com.dino.message.threadfeature.domain.model.Post
import kotlinx.serialization.Serializable

@Serializable
data class GetPostResponse(
    val post: Post
) : ResponseToResultMapper<Post> {
    override fun mapResponseToResult(): Post = post
}
