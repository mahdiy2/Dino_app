package com.dino.message.threadfeature.domain.usecase

import com.dino.message.corefeature.presentation.util.Resource
import com.dino.message.threadfeature.domain.model.BlockedUserResult
import com.dino.message.threadfeature.domain.repository.ThreadRepository
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class BlockUserUseCase @Inject constructor(
    private val threadRepository: ThreadRepository
) {
    fun execute(username: String) = flow<Resource<BlockedUserResult>> {
        emit(Resource.Loading())
        emit(threadRepository.blockUser(username))
    }
}
