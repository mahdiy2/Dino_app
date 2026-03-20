package com.dino.message.searchfeature.data.remote.dto

import com.dino.message.corefeature.data.remote.ResponseToResultMapper
import com.dino.message.searchfeature.domain.model.SearchPostsResult
import com.dino.message.threadfeature.domain.model.Post
import kotlinx.serialization.Serializable

@Serializable
data class SearchPostsResponse(
    val page: Int,
    val limit: Int,
    val total: Int,
    val posts: List<Post>
) : ResponseToResultMapper<SearchPostsResult> {
    override fun mapResponseToResult(): SearchPostsResult = SearchPostsResult(
        page = page,
        limit = limit,
        total = total,
        posts = posts
    )
}
