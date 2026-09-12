package com.dino.message.searchfeature.domain.model

data class SearchUsersResult(
    val page: Int,
    val limit: Int,
    val total: Int,
    val users: List<SearchUser>
)
