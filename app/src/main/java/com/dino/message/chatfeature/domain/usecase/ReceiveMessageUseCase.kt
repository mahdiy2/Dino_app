package com.dino.message.chatfeature.domain.usecase

import com.dino.message.chatfeature.domain.repository.ChatRepository
import javax.inject.Inject

class ReceiveMessageUseCase @Inject constructor(
    private val socketRepository: ChatRepository
) {
//
//    fun onNewMessage(block: (Message) -> Unit) {
//        socketRepository.onNewMessage(block)
//    }
}