package com.dino.message.threadfeature.data.repository

import com.dino.message.corefeature.domain.result.Resource
import com.dino.message.threadfeature.data.remote.ThreadApiService
import com.dino.message.threadfeature.domain.model.BlockedUserResult
import com.dino.message.threadfeature.domain.model.Comment
import com.dino.message.threadfeature.domain.model.DeleteContentResult
import com.dino.message.threadfeature.domain.model.CreatedComment
import com.dino.message.threadfeature.domain.model.CreatedPost
import com.dino.message.threadfeature.domain.model.CreatePostMedia
import com.dino.message.threadfeature.domain.model.FeedMode
import com.dino.message.threadfeature.domain.model.FeedPage
import com.dino.message.threadfeature.domain.model.Post
import com.dino.message.threadfeature.domain.model.PlaylistPage
import com.dino.message.threadfeature.domain.model.PlaylistTrackMutation
import com.dino.message.threadfeature.domain.model.StoryBundle
import com.dino.message.threadfeature.domain.model.StoryFeed
import com.dino.message.threadfeature.domain.model.StoryLikeResult
import com.dino.message.threadfeature.domain.model.StoryReplyResult
import com.dino.message.threadfeature.domain.model.StoryUploadResult
import com.dino.message.threadfeature.domain.model.StoryViewResult
import com.dino.message.threadfeature.domain.model.StoryViewersPage
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

    override suspend fun editPost(
        postId: Long,
        text: String,
        mediaFiles: List<CreatePostMedia>,
        keepMediaIds: List<Long>,
        followersOnly: Boolean?
    ): Resource<DeleteContentResult> =
        threadApiService.editPost(
            postId = postId,
            text = text,
            mediaFiles = mediaFiles,
            keepMediaIds = keepMediaIds,
            followersOnly = followersOnly
        )

    override suspend fun deletePost(postId: Long): Resource<DeleteContentResult> =
        threadApiService.deletePost(postId = postId)

    override suspend fun getPost(postId: Long): Resource<Post> =
        threadApiService.getPost(postId = postId)

    override suspend fun getFeed(
        mode: FeedMode,
        cursor: String?,
        limit: Int
    ): Resource<FeedPage> =
        threadApiService.getFeed(
            mode = mode,
            cursor = cursor,
            limit = limit
        )

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

    override suspend fun uploadStory(
        mediaFile: CreatePostMedia,
        mentionUsernames: List<String>
    ): Resource<StoryUploadResult> =
        threadApiService.uploadStory(
            mediaFile = mediaFile,
            mentionUsernames = mentionUsernames
        )

    override suspend fun getStoryFeed(): Resource<StoryFeed> =
        threadApiService.getStoryFeed()

    override suspend fun getStoryBundle(storyId: Long): Resource<StoryBundle> =
        threadApiService.getStoryBundle(storyId = storyId)

    override suspend fun deleteStory(storyId: Long): Resource<DeleteContentResult> =
        threadApiService.deleteStory(storyId = storyId)

    override suspend fun recordStoryView(storyId: Long): Resource<StoryViewResult> =
        threadApiService.recordStoryView(storyId = storyId)

    override suspend fun setStoryLike(storyId: Long, liked: Boolean): Resource<StoryLikeResult> =
        threadApiService.setStoryLike(
            storyId = storyId,
            liked = liked
        )

    override suspend fun getStoryViewers(
        storyId: Long,
        page: Int,
        limit: Int
    ): Resource<StoryViewersPage> =
        threadApiService.getStoryViewers(
            storyId = storyId,
            page = page,
            limit = limit
        )

    override suspend fun replyToStory(storyId: Long, replyText: String): Resource<StoryReplyResult> =
        threadApiService.replyToStory(
            storyId = storyId,
            replyText = replyText
        )

    override suspend fun getStoryMentionCandidates(
        query: String,
        limit: Int
    ): Resource<List<String>> =
        threadApiService.getStoryMentionCandidates(
            query = query,
            limit = limit
        )
}
