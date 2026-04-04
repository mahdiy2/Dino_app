package com.dino.message.chatfeature.domain.repository

import android.net.Uri
import com.dino.message.chatfeature.data.remote.dto.MessageAttachment
import com.dino.message.chatfeature.data.remote.dto.MessageSent
import com.dino.message.chatfeature.data.remote.dto.NewMessage
import com.dino.message.chatfeature.data.remote.dto.PendingMessage
import com.dino.message.chatfeature.data.remote.dto.UnreadResponse
import com.dino.message.chatfeature.domain.model.Inbox
import com.dino.message.chatfeature.domain.model.MessageEntity
import com.dino.message.chatfeature.domain.model.ReceiverPublicKey
import com.dino.message.chatfeature.domain.model.SendMessage
import com.dino.message.corefeature.presentation.util.Resource

interface ChatRepository {
    suspend fun getInbox(): Resource<List<Inbox>?>

    suspend fun getReceiverPublicKey(username: String): Resource<ReceiverPublicKey>

    suspend fun sendMessage(to: String, message: String, encryptedKey: String): Resource<SendMessage>

    suspend fun uploadAttachment(fileUri: Uri): Resource<MessageAttachment>

    suspend fun saveMessage(message: MessageEntity)
    suspend fun getMessages(chatId: String): List<MessageEntity>
    suspend fun clearMessages(chatId: String)
    suspend fun deleteMessageByLocalId(localId: Int)
    suspend fun deleteMessageByMessageId(messageId: String)
    suspend fun markMessageSeenByMessageId(messageId: String)

    suspend fun getUnread(): Resource<UnreadResponse>
}
