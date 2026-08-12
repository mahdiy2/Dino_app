package com.dino.message.threadfeature.domain.usecase

import com.dino.message.corefeature.domain.result.Resource
import com.dino.message.threadfeature.domain.model.StoryViewResult
import com.dino.message.threadfeature.domain.repository.ThreadRepository
import kotlinx.coroutines.flow.flow

class RecordStoryViewUseCase constructor(
    private val threadRepository: ThreadRepository
) {
    fun execute(storyId: Long) = flow<Resource<StoryViewResult>> {
        emit(Resource.Loading())
        emit(threadRepository.recordStoryView(storyId))
    }
}
