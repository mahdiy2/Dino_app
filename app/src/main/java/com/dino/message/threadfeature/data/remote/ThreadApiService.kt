package com.dino.message.threadfeature.data.remote

import com.dino.message.corefeature.presentation.util.Resource
import com.dino.message.threadfeature.domain.model.BlockedUserResult
import com.dino.message.threadfeature.domain.model.Comment
import com.dino.message.threadfeature.domain.model.CreatedComment
import com.dino.message.threadfeature.domain.model.CreatedPost
import com.dino.message.threadfeature.domain.model.CreatePostMedia
import com.dino.message.threadfeature.domain.model.PostsPage
import com.dino.message.threadfeature.domain.model.VoteResult

interface ThreadApiService {
    suspend fun createPost(text: String, mediaFiles: List<CreatePostMedia> = emptyList()): Resource<CreatedPost>

    suspend fun getPosts(page: Int, limit: Int): Resource<PostsPage>

    suspend fun getComments(postId: Long, page: Int, pageSize: Int): Resource<List<Comment>>

    suspend fun createComment(postId: Long, text: String, parentId: Long?): Resource<CreatedComment>

    suspend fun vote(targetType: String, targetId: Long, value: Long): Resource<VoteResult>

    suspend fun blockUser(username: String): Resource<BlockedUserResult>
}
