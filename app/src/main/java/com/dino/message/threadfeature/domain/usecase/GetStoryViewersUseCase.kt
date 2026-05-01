package com.dino.message.threadfeature.domain.usecase

import com.dino.message.corefeature.presentation.util.Resource
import com.dino.message.threadfeature.domain.model.StoryViewersPage
import com.dino.message.threadfeature.domain.repository.ThreadRepository
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class GetStoryViewersUseCase @Inject constructor(
    private val threadRepository: ThreadRepository
) {
    fun execute(storyId: Long, page: Int, limit: Int) = flow<Resource<StoryViewersPage>> {
        emit(Resource.Loading())
        emit(threadRepository.getStoryViewers(storyId, page, limit))
    }
}
