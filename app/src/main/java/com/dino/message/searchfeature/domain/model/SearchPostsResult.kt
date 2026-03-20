package com.dino.message.searchfeature.domain.model

import com.dino.message.threadfeature.domain.model.Post

data class SearchPostsResult(
    val page: Int,
    val limit: Int,
    val total: Int,
    val posts: List<Post>
)
