package com.dino.message.threadfeature.domain.repository

import com.dino.message.corefeature.domain.result.Resource
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

interface ThreadRepository {
    suspend fun createPost(
        text: String,
        mediaFiles: List<CreatePostMedia> = emptyList(),
        followersOnly: Boolean = false,
        quotedPostId: Long? = null
    ): Resource<CreatedPost>

    suspend fun editPost(
        postId: Long,
        text: String,
        mediaFiles: List<CreatePostMedia> = emptyList(),
        keepMediaIds: List<Long> = emptyList(),
        followersOnly: Boolean? = null
    ): Resource<DeleteContentResult>

    suspend fun deletePost(postId: Long): Resource<DeleteContentResult>

    suspend fun getPost(postId: Long): Resource<Post>

    suspend fun getFeed(mode: FeedMode, cursor: String?, limit: Int): Resource<FeedPage>

    suspend fun getComments(postId: Long, page: Int, pageSize: Int): Resource<List<Comment>>

    suspend fun createComment(postId: Long, text: String, parentId: Long?): Resource<CreatedComment>

    suspend fun deleteComment(commentId: Long): Resource<DeleteContentResult>

    suspend fun vote(targetType: String, targetId: Long, value: Long): Resource<VoteResult>

    suspend fun blockUser(username: String): Resource<BlockedUserResult>

    suspend fun addTrackToPlaylist(mediaId: Long): Resource<PlaylistTrackMutation>

    suspend fun removeTrackFromPlaylist(trackId: Long): Resource<DeleteContentResult>

    suspend fun getPlaylistTracks(page: Int, limit: Int): Resource<PlaylistPage>

    suspend fun isTrackInPlaylist(trackUrl: String): Resource<Boolean>

    suspend fun uploadStory(
        mediaFile: CreatePostMedia,
        mentionUsernames: List<String> = emptyList()
    ): Resource<StoryUploadResult>

    suspend fun getStoryFeed(): Resource<StoryFeed>

    suspend fun getStoryBundle(storyId: Long): Resource<StoryBundle>

    suspend fun deleteStory(storyId: Long): Resource<DeleteContentResult>

    suspend fun recordStoryView(storyId: Long): Resource<StoryViewResult>

    suspend fun setStoryLike(storyId: Long, liked: Boolean): Resource<StoryLikeResult>

    suspend fun getStoryViewers(
        storyId: Long,
        page: Int,
        limit: Int
    ): Resource<StoryViewersPage>

    suspend fun replyToStory(storyId: Long, replyText: String): Resource<StoryReplyResult>

    suspend fun getStoryMentionCandidates(
        query: String,
        limit: Int = 6
    ): Resource<List<String>>
}
