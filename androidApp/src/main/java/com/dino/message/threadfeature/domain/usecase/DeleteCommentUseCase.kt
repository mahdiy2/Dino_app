package com.dino.message.threadfeature.domain.usecase

import com.dino.message.corefeature.presentation.util.Resource
import com.dino.message.threadfeature.domain.model.DeleteContentResult
import com.dino.message.threadfeature.domain.repository.ThreadRepository
import kotlinx.coroutines.flow.flow

class DeleteCommentUseCase constructor(
    private val threadRepository: ThreadRepository
) {
    fun execute(commentId: Long) = flow<Resource<DeleteContentResult>> {
        emit(Resource.Loading())
        emit(threadRepository.deleteComment(commentId = commentId))
    }
}
