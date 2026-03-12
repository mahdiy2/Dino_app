package com.dino.message.threadfeature.data.repository

import com.dino.message.corefeature.presentation.util.Resource
import com.dino.message.threadfeature.data.remote.ThreadApiService
import com.dino.message.threadfeature.domain.model.Comment
import com.dino.message.threadfeature.domain.model.CreatedComment
import com.dino.message.threadfeature.domain.model.CreatedPost
import com.dino.message.threadfeature.domain.model.CreatePostMedia
import com.dino.message.threadfeature.domain.model.PostsPage
import com.dino.message.threadfeature.domain.model.VoteResult
import com.dino.message.threadfeature.domain.repository.ThreadRepository

class ThreadRepositoryImpl(
    private val threadApiService: ThreadApiService
) : ThreadRepository {
    override suspend fun createPost(
        text: String,
        mediaFiles: List<CreatePostMedia>
    ): Resource<CreatedPost> =
        threadApiService.createPost(text = text, mediaFiles = mediaFiles)

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

    override suspend fun vote(targetType: String, targetId: Long, value: Long): Resource<VoteResult> =
        threadApiService.vote(targetType = targetType, targetId = targetId, value = value)
}
