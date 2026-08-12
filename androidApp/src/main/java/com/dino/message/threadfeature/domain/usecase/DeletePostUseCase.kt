package com.dino.message.threadfeature.domain.usecase

import com.dino.message.corefeature.domain.result.Resource
import com.dino.message.threadfeature.domain.model.DeleteContentResult
import com.dino.message.threadfeature.domain.repository.ThreadRepository
import kotlinx.coroutines.flow.flow

class DeletePostUseCase constructor(
    private val threadRepository: ThreadRepository
) {
    fun execute(postId: Long) = flow<Resource<DeleteContentResult>> {
        emit(Resource.Loading())
        emit(threadRepository.deletePost(postId = postId))
    }
}
