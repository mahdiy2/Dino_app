package com.dino.message.chatfeature.domain.usecase

import com.dino.message.chatfeature.domain.repository.ChatRepository
import com.dino.message.corefeature.presentation.util.Resource
import kotlinx.coroutines.flow.flow

class GetInboxUseCase constructor(
    private val chatRepository: ChatRepository
) {
    fun execute() = flow {
        emit(Resource.Loading())
        emit(chatRepository.getInbox())
    }
}