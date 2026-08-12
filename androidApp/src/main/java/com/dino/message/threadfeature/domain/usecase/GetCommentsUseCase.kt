package com.dino.message.threadfeature.domain.usecase

import com.dino.message.corefeature.domain.result.Resource
import com.dino.message.threadfeature.domain.model.Comment
import com.dino.message.threadfeature.domain.repository.ThreadRepository
import kotlinx.coroutines.flow.flow

class GetCommentsUseCase constructor(
    private val threadRepository: ThreadRepository
) {
    fun execute(postId: Long, page: Int, pageSize: Int) = flow<Resource<List<Comment>>> {
        emit(Resource.Loading())
        emit(threadRepository.getComments(postId = postId, page = page, pageSize = pageSize))
    }
}
