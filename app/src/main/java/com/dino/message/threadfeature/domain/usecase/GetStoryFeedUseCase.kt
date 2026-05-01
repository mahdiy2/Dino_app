package com.dino.message.threadfeature.domain.usecase

import com.dino.message.corefeature.presentation.util.Resource
import com.dino.message.threadfeature.domain.model.StoryFeed
import com.dino.message.threadfeature.domain.repository.ThreadRepository
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class GetStoryFeedUseCase @Inject constructor(
    private val threadRepository: ThreadRepository
) {
    fun execute() = flow<Resource<StoryFeed>> {
        emit(Resource.Loading())
        emit(threadRepository.getStoryFeed())
    }
}
