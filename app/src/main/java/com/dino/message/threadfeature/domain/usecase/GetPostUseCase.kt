package com.dino.message.threadfeature.domain.usecase

import com.dino.message.corefeature.presentation.util.Resource
import com.dino.message.threadfeature.domain.model.Post
import com.dino.message.threadfeature.domain.repository.ThreadRepository
import kotlinx.coroutines.flow.flow

class GetPostUseCase constructor(
    private val threadRepository: ThreadRepository
) {
    fun execute(postId: Long) = flow<Resource<Post>> {
        emit(Resource.Loading())
        emit(threadRepository.getPost(postId = postId))
    }
}
