package com.dino.message.threadfeature.data.remote

import com.dino.message.corefeature.data.remote.BaseRoute

sealed class ThreadRoute(url: String) : BaseRoute(url) {
    object CreatePost : ThreadRoute("api/posts")

    data class DeletePost(val postId: Long) : ThreadRoute("api/posts/$postId")

    data class GetPost(val postId: Long) : ThreadRoute("api/posts/$postId")

    data class GetPosts(val page: Int, val limit: Int) : ThreadRoute("api/posts?page=$page&limit=$limit")

    data class GetComments(
        val postId: Long,
        val page: Int,
        val pageSize: Int
    ) : ThreadRoute("api/posts/$postId/comments?page=$page&page_size=$pageSize")

    data class CreateComment(val postId: Long) : ThreadRoute("api/posts/$postId/comments")

    data class DeleteComment(val commentId: Long) : ThreadRoute("api/comments/$commentId")

    object Vote : ThreadRoute("api/votes")

    data class BlockUser(val username: String) : ThreadRoute("api/blocks/$username")

    object PlaylistTracks : ThreadRoute("api/playlists/tracks")
    data class DeletePlaylistTrack(val trackId: Long) : ThreadRoute("api/playlists/tracks/$trackId")
    object PlaylistTrackExists : ThreadRoute("api/playlists/tracks/exists")

    data class GetPlaylistTracks(
        val page: Int,
        val limit: Int
    ) : ThreadRoute("api/playlists/tracks?page=$page&limit=$limit")
}
