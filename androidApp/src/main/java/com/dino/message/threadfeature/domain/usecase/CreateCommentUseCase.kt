package com.dino.message.threadfeature.domain.usecase

import com.dino.message.corefeature.presentation.util.Resource
import com.dino.message.threadfeature.domain.model.CreatedComment
import com.dino.message.threadfeature.domain.repository.ThreadRepository
import kotlinx.coroutines.flow.flow

class CreateCommentUseCase constructor(
    private val threadRepository: ThreadRepository
) {
    fun execute(postId: Long, text: String, parentId: Long?) = flow<Resource<CreatedComment>> {
        emit(Resource.Loading())
        emit(threadRepository.createComment(postId = postId, text = text, parentId = parentId))
    }
}
