package com.dino.message.chatfeature.domain.usecase

import com.dino.message.chatfeature.domain.repository.ChatRepository
import javax.inject.Inject

class ConnectUseCase @Inject constructor(
    private val socketRepository: ChatRepository
) {

    suspend operator fun invoke() {
        socketRepository.connectToSocket()
    }
}