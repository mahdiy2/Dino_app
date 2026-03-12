package com.dino.message.chatfeature.domain.repository

import com.dino.message.chatfeature.domain.model.Inbox
import com.dino.message.chatfeature.domain.model.MessageEntity
import com.dino.message.chatfeature.domain.model.ReceiverPublicKey
import com.dino.message.chatfeature.domain.model.SendMessage
import com.dino.message.chatfeature.data.remote.dto.MessageSent
import com.dino.message.chatfeature.data.remote.dto.NewMessage
import com.dino.message.chatfeature.data.remote.dto.PendingMessage
import com.dino.message.corefeature.presentation.util.Resource

interface ChatRepository {
    suspend fun getInbox(): Resource<List<Inbox>?>

    suspend fun getReceiverPublicKey(username: String): Resource<ReceiverPublicKey>

    suspend fun sendMessage(to: String, message: String, encryptedKey: String): Resource<SendMessage>


    // db part
    suspend fun saveMessage(message: MessageEntity)
    suspend fun getMessages(): List<MessageEntity>
    suspend fun clearMessages()
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
