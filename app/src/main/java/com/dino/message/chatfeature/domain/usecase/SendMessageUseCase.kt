package com.dino.message.chatfeature.domain.usecase

import com.dino.message.chatfeature.domain.repository.ChatRepository
import javax.inject.Inject

class SendMessageUseCase @Inject constructor(
    private val chatRepository: ChatRepository
) {
    fun execute(
        to: String,
        message: String,
        encryptedKey: String
    ) {
        chatRepository.sendMessageWithSocket(
            to = to,
            message = message,
            encryptedKey = encryptedKey
        )
    }
}
