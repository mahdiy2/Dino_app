package com.dino.message.chatfeature.domain.usecase

import com.dino.message.chatfeature.domain.repository.ChatRepository
import kotlinx.coroutines.flow.flow

class GetMessageFromDBUseCase constructor(
    private val chatRepository: ChatRepository
) {
    fun execute(chatId: String, limit: Int, offset: Int = 0) = flow {
        emit(chatRepository.getMessages(chatId = chatId, limit = limit, offset = offset))
    }
}
