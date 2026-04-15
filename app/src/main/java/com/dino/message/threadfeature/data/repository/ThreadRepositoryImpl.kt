package com.dino.message.threadfeature.data.repository

import com.dino.message.corefeature.presentation.util.Resource
import com.dino.message.threadfeature.data.remote.ThreadApiService
import com.dino.message.threadfeature.domain.model.BlockedUserResult
import com.dino.message.threadfeature.domain.model.Comment
import com.dino.message.threadfeature.domain.model.DeleteContentResult
import com.dino.message.threadfeature.domain.model.CreatedComment
import com.dino.message.threadfeature.domain.model.CreatedPost
import com.dino.message.threadfeature.domain.model.CreatePostMedia
import com.dino.message.threadfeature.domain.model.Post
import com.dino.message.threadfeature.domain.model.PostsPage
import com.dino.message.threadfeature.domain.model.PlaylistPage
import com.dino.message.threadfeature.domain.model.PlaylistTrackMutation
import com.dino.message.threadfeature.domain.model.VoteResult
import com.dino.message.threadfeature.domain.repository.ThreadRepository

class ThreadRepositoryImpl(
    private val threadApiService: ThreadApiService
) : ThreadRepository {
    override suspend fun createPost(
        text: String,
        mediaFiles: List<CreatePostMedia>,
        followersOnly: Boolean,
        quotedPostId: Long?
    ): Resource<CreatedPost> =
        threadApiService.createPost(
            text = text,
            mediaFiles = mediaFiles,
            followersOnly = followersOnly,
            quotedPostId = quotedPostId
        )

    override suspend fun deletePost(postId: Long): Resource<DeleteContentResult> =
        threadApiService.deletePost(postId = postId)

    override suspend fun getPost(postId: Long): Resource<Post> =
        threadApiService.getPost(postId = postId)

    override suspend fun getPosts(page: Int, limit: Int): Resource<PostsPage> =
        threadApiService.getPosts(page = page, limit = limit)

    override suspend fun getComments(
        postId: Long,
        page: Int,
        pageSize: Int
    ): Resource<List<Comment>> =
        threadApiService.getComments(postId = postId, page = page, pageSize = pageSize)

    override suspend fun createComment(
        postId: Long,
        text: String,
        parentId: Long?
    ): Resource<CreatedComment> =
        threadApiService.createComment(postId = postId, text = text, parentId = parentId)

    override suspend fun deleteComment(commentId: Long): Resource<DeleteContentResult> =
        threadApiService.deleteComment(commentId = commentId)

    override suspend fun vote(targetType: String, targetId: Long, value: Long): Resource<VoteResult> =
        threadApiService.vote(targetType = targetType, targetId = targetId, value = value)

    override suspend fun blockUser(username: String): Resource<BlockedUserResult> =
        threadApiService.blockUser(username = username)

    override suspend fun addTrackToPlaylist(mediaId: Long): Resource<PlaylistTrackMutation> =
        threadApiService.addTrackToPlaylist(mediaId = mediaId)

    override suspend fun removeTrackFromPlaylist(trackId: Long): Resource<DeleteContentResult> =
        threadApiService.removeTrackFromPlaylist(trackId = trackId)

    override suspend fun getPlaylistTracks(page: Int, limit: Int): Resource<PlaylistPage> =
        threadApiService.getPlaylistTracks(page = page, limit = limit)

    override suspend fun isTrackInPlaylist(trackUrl: String): Resource<Boolean> =
        threadApiService.isTrackInPlaylist(trackUrl = trackUrl)
}
