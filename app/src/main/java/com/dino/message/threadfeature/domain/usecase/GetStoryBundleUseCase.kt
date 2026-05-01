package com.dino.message.threadfeature.domain.usecase

import com.dino.message.corefeature.presentation.util.Resource
import com.dino.message.threadfeature.domain.model.StoryBundle
import com.dino.message.threadfeature.domain.repository.ThreadRepository
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class GetStoryBundleUseCase @Inject constructor(
    private val threadRepository: ThreadRepository
) {
    fun execute(storyId: Long) = flow<Resource<StoryBundle>> {
        emit(Resource.Loading())
        emit(threadRepository.getStoryBundle(storyId))
    }
}
