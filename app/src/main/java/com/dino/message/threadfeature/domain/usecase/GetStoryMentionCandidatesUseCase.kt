package com.dino.message.threadfeature.domain.usecase

import com.dino.message.corefeature.presentation.util.Resource
import com.dino.message.threadfeature.domain.repository.ThreadRepository
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class GetStoryMentionCandidatesUseCase @Inject constructor(
    private val threadRepository: ThreadRepository
) {
    fun execute(query: String, limit: Int = 6) = flow<Resource<List<String>>> {
        emit(Resource.Loading())
        emit(threadRepository.getStoryMentionCandidates(query = query, limit = limit))
    }
}
