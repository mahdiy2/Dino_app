package com.dino.message.chatfeature.domain.usecase

import com.dino.message.chatfeature.domain.model.MessageEntity
import com.dino.message.chatfeature.domain.repository.ChatRepository
import kotlinx.coroutines.flow.flow

class SaveMessageInDbUseCase constructor(
    private val chatRepository: ChatRepository
) {
    fun execute(message: MessageEntity) = flow {
        emit(chatRepository.saveMessage(message))
    }
}