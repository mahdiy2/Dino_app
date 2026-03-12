package com.dino.message.chatfeature.data.remote

import com.dino.message.chatfeature.domain.model.Inbox
import com.dino.message.chatfeature.domain.model.ReceiverPublicKey
import com.dino.message.chatfeature.domain.model.SendMessage
import com.dino.message.chatfeature.data.remote.dto.MessageSent
import com.dino.message.chatfeature.data.remote.dto.NewMessage
import com.dino.message.chatfeature.data.remote.dto.PendingMessage
import com.dino.message.corefeature.presentation.util.Resource

interface ChatApiService {
    suspend fun getInbox(): Resource<List<Inbox>?>

    suspend fun getReceiverPublicKey(username: String): Resource<ReceiverPublicKey>

    suspend fun sendMessage(
        to: String,
        message: String,
        encryptedKey: String
    ): Resource<SendMessage>

    suspend fun connectToSocket(
        onConnected: (String) -> Unit,
        onPendingMessages: (List<PendingMessage>) -> Unit,
        onNewMessage: (NewMessage) -> Unit,
        onMessageSent: (MessageSent) -> Unit,
        onMessageError: (String) -> Unit,
        onUserStatus: (username: String, online: Boolean) -> Unit
    )
    fun sendMessageWithSocket(to: String, message: String, encryptedKey: String)
    fun getUserStatus(username: String)
}
