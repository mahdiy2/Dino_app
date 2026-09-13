package com.dino.message.chatfeature.data.remote

import com.dino.message.chatfeature.data.remote.dto.MessageAttachment
import com.dino.message.chatfeature.data.remote.dto.UnreadResponse
import com.dino.message.corefeature.domain.upload.ReadableMediaFile
import com.dino.message.chatfeature.domain.model.Inbox
import com.dino.message.chatfeature.domain.model.PrivateHistoryPage
import com.dino.message.chatfeature.domain.model.ReceiverPublicKey
import com.dino.message.chatfeature.domain.model.SendMessage
import com.dino.message.corefeature.domain.result.Resource

interface ChatApiService {
    suspend fun getInbox(): Resource<List<Inbox>?>

    suspend fun getPrivateHistory(
        chatId: String,
        limit: Int,
        before: String? = null,
    ): Resource<PrivateHistoryPage>

    suspend fun getReceiverPublicKey(username: String): Resource<ReceiverPublicKey>

    suspend fun sendMessage(
        to: String,
        message: String,
        encryptedKey: String
    ): Resource<SendMessage>

    suspend fun uploadAttachment(file: ReadableMediaFile): Resource<MessageAttachment>
    suspend fun reactToMessage(messageId: String, emoji: String?): Resource<Unit>

    suspend fun getUnread(): Resource<UnreadResponse>
}
