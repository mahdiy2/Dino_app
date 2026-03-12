package com.dino.message.threadfeature.data.remote

import com.dino.message.corefeature.data.remote.BaseRoute

sealed class ThreadRoute(url: String) : BaseRoute(url) {
    object CreatePost : ThreadRoute("api/posts")

    data class GetPosts(val page: Int, val limit: Int) : ThreadRoute("api/posts?page=$page&limit=$limit")

    data class GetComments(
        val postId: Long,
        val page: Int,
        val pageSize: Int
    ) : ThreadRoute("api/posts/$postId/comments?page=$page&page_size=$pageSize")

    data class CreateComment(val postId: Long) : ThreadRoute("api/posts/$postId/comments")

    object Vote : ThreadRoute("api/votes")
}
