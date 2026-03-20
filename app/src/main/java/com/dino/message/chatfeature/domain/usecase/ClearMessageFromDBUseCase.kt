package com.dino.message.chatfeature.domain.usecase

import com.dino.message.chatfeature.domain.repository.ChatRepository
import jakarta.inject.Inject
import kotlinx.coroutines.flow.flow

class ClearMessageFromDBUseCase @Inject constructor(
    private val chatRepository: ChatRepository
) {
    fun execute(chatId: String) = flow {
        emit(chatRepository.clearMessages(chatId))
    }
}
