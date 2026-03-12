package com.dino.message.chatfeature.domain.usecase

import com.dino.message.chatfeature.domain.repository.ChatRepository
import com.dino.message.chatfeature.data.remote.dto.MessageSent
import com.dino.message.chatfeature.data.remote.dto.NewMessage
import com.dino.message.chatfeature.data.remote.dto.PendingMessage
import javax.inject.Inject

class ConnectUseCase @Inject constructor(
    private val socketRepository: ChatRepository
) {

    suspend operator fun invoke(
        onConnected: (String) -> Unit,
        onPendingMessages: (List<PendingMessage>) -> Unit,
        onNewMessage: (NewMessage) -> Unit,
        onMessageSent: (MessageSent) -> Unit,
        onMessageError: (String) -> Unit,
        onUserStatus: (username: String, online: Boolean) -> Unit
    ) {
        socketRepository.connectToSocket(
            onConnected = onConnected,
            onPendingMessages = onPendingMessages,
            onNewMessage = onNewMessage,
            onMessageSent = onMessageSent,
            onMessageError = onMessageError,
            onUserStatus = onUserStatus
        )
    }

    fun getUserStatus(username: String) {
        socketRepository.getUserStatus(username)
    }
}
