package com.dino.message.searchfeature.data.remote.dto

import com.dino.message.corefeature.data.remote.ResponseToResultMapper
import com.dino.message.searchfeature.domain.model.SearchAllResult
import com.dino.message.searchfeature.domain.model.SearchUser
import com.dino.message.threadfeature.domain.model.Post
import kotlinx.serialization.Serializable

@Serializable
data class SearchAllResponse(
    val page: Int,
    val limit: Int,
    val users: List<SearchUser>,
    val users_total: Int,
    val posts: List<Post>,
    val posts_total: Int
) : ResponseToResultMapper<SearchAllResult> {
    override fun mapResponseToResult(): SearchAllResult = SearchAllResult(
        page = page,
        limit = limit,
        users = users,
        usersTotal = users_total,
        posts = posts,
        postsTotal = posts_total
    )
}
