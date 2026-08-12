package com.dino.message.threadfeature.domain.usecase

import com.dino.message.corefeature.domain.result.Resource
import com.dino.message.threadfeature.domain.model.DeleteContentResult
import com.dino.message.threadfeature.domain.repository.ThreadRepository
import kotlinx.coroutines.flow.flow

class DeleteStoryUseCase constructor(
    private val threadRepository: ThreadRepository
) {
    fun execute(storyId: Long) = flow<Resource<DeleteContentResult>> {
        emit(Resource.Loading())
        emit(threadRepository.deleteStory(storyId))
    }
}
