package com.dino.message.threadfeature.data.remote

import com.dino.message.corefeature.data.remote.BaseRoute

sealed class ThreadRoute(url: String) : BaseRoute(url) {
    object CreatePost : ThreadRoute("api/posts")

    data class DeletePost(val postId: Long) : ThreadRoute("api/posts/$postId")

    data class GetPost(val postId: Long) : ThreadRoute("api/posts/$postId")

    data class GetPosts(val page: Int, val limit: Int) : ThreadRoute("api/posts?page=$page&limit=$limit")
    object GetFeed : ThreadRoute("api/posts/feed")

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
    object UploadStory : ThreadRoute("api/story/upload")
    object StoryFeed : ThreadRoute("api/story/feed")
    data class StoryById(val storyId: Long) : ThreadRoute("api/story/$storyId")
    data class DeleteStory(val storyId: Long) : ThreadRoute("api/story/$storyId")
    object StoryView : ThreadRoute("api/story/view")
    object StoryLike : ThreadRoute("api/story/like")
    object StoryReply : ThreadRoute("api/story/reply")
    object StoryMentions : ThreadRoute("api/story/mentions")
    data class StoryViewers(
        val storyId: Long,
        val page: Int,
        val limit: Int
    ) : ThreadRoute("api/story/$storyId/viewers?page=$page&limit=$limit")

    data class GetPlaylistTracks(
        val page: Int,
        val limit: Int
    ) : ThreadRoute("api/playlists/tracks?page=$page&limit=$limit")
}
