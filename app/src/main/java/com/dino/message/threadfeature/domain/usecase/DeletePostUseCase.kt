package com.dino.message.threadfeature.domain.usecase

import com.dino.message.corefeature.presentation.util.Resource
import com.dino.message.threadfeature.domain.model.DeleteContentResult
import com.dino.message.threadfeature.domain.repository.ThreadRepository
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class DeletePostUseCase @Inject constructor(
    private val threadRepository: ThreadRepository
) {
    fun execute(postId: Long) = flow<Resource<DeleteContentResult>> {
        emit(Resource.Loading())
        emit(threadRepository.deletePost(postId = postId))
    }
}
