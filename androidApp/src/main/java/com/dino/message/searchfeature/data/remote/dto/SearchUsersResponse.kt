package com.dino.message.searchfeature.data.remote.dto

import com.dino.message.corefeature.data.remote.ResponseToResultMapper
import com.dino.message.searchfeature.domain.model.SearchUser
import com.dino.message.searchfeature.domain.model.SearchUsersResult
import kotlinx.serialization.Serializable

@Serializable
data class SearchUsersResponse(
    val page: Int,
    val limit: Int,
    val total: Int,
    val users: List<SearchUser>
) : ResponseToResultMapper<SearchUsersResult> {
    override fun mapResponseToResult(): SearchUsersResult = SearchUsersResult(
        page = page,
        limit = limit,
        total = total,
        users = users
    )
}
