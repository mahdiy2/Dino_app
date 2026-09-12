package com.dino.message.searchfeature.domain.model

import com.dino.message.threadfeature.domain.model.Post

data class SearchAllResult(
    val page: Int,
    val limit: Int,
    val users: List<SearchUser>,
    val usersTotal: Int,
    val posts: List<Post>,
    val postsTotal: Int
)
