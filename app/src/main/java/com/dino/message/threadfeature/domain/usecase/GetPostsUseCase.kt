package com.dino.message.threadfeature.domain.usecase

import com.dino.message.corefeature.presentation.util.Resource
import com.dino.message.threadfeature.domain.model.FeedMode
import com.dino.message.threadfeature.domain.model.FeedPage
import com.dino.message.threadfeature.domain.repository.ThreadRepository
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class GetPostsUseCase @Inject constructor(
    private val threadRepository: ThreadRepository
) {
    fun execute(
        mode: FeedMode,
        cursor: String?,
        limit: Int
    ) = flow<Resource<FeedPage>> {
        emit(Resource.Loading())
        emit(
            threadRepository.getFeed(
                mode = mode,
                cursor = cursor,
                limit = limit
            )
        )
    }
}
