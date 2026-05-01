package com.dino.message.threadfeature.domain.usecase

import com.dino.message.corefeature.presentation.util.Resource
import com.dino.message.threadfeature.domain.model.StoryLikeResult
import com.dino.message.threadfeature.domain.repository.ThreadRepository
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class SetStoryLikeUseCase @Inject constructor(
    private val threadRepository: ThreadRepository
) {
    fun execute(storyId: Long, liked: Boolean) = flow<Resource<StoryLikeResult>> {
        emit(Resource.Loading())
        emit(threadRepository.setStoryLike(storyId, liked))
    }
}
