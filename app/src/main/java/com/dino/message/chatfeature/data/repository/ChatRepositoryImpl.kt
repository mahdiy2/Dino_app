package com.dino.message.chatfeature.data.repository

import android.net.Uri
import com.dino.message.chatfeature.data.db.MessageDao
import com.dino.message.chatfeature.data.local.toDomainEntity
import com.dino.message.chatfeature.data.local.toLocalEntity
import com.dino.message.chatfeature.data.remote.ChatApiService
import com.dino.message.chatfeature.data.remote.dto.MessageAttachment
import com.dino.message.chatfeature.data.remote.dto.MessageSent
import com.dino.message.chatfeature.data.remote.dto.NewMessage
import com.dino.message.chatfeature.data.remote.dto.PendingMessage
import com.dino.message.chatfeature.data.remote.dto.UnreadResponse
import com.dino.message.chatfeature.domain.model.Inbox
import com.dino.message.chatfeature.domain.model.MessageEntity
import com.dino.message.chatfeature.domain.model.PrivateHistoryPage
import com.dino.message.chatfeature.domain.model.ReceiverPublicKey
import com.dino.message.chatfeature.domain.model.SendMessage
import com.dino.message.chatfeature.domain.repository.ChatRepository
import com.dino.message.corefeature.presentation.util.Resource

class ChatRepositoryImpl(
    private val chatApiService: ChatApiService,
    private val dao: MessageDao
) : ChatRepository {

    override suspend fun getInbox(): Resource<List<Inbox>?> =
        chatApiService.getInbox()

    override suspend fun getPrivateHistory(
        chatId: String,
        limit: Int,
        before: String?,
    ): Resource<PrivateHistoryPage> = chatApiService.getPrivateHistory(
        chatId = chatId,
        limit = limit,
        before = before,
    )

    override suspend fun getReceiverPublicKey(username: String): Resource<ReceiverPublicKey> =
        chatApiService.getReceiverPublicKey(username)

     override suspend fun sendMessage(
         to: String,
         message: String,
         encryptedKey: String
     ): Resource<SendMessage> = chatApiService.sendMessage(to, message, encryptedKey)

    override suspend fun uploadAttachment(fileUri: Uri): Resource<MessageAttachment> =
        chatApiService.uploadAttachment(fileUri)

    override suspend fun saveMessage(message: MessageEntity) = dao.insertMessage(message.toLocalEntity())
    override suspend fun getMessages(chatId: String, limit: Int, offset: Int) =
        dao.getMessagesByChatId(chatId = chatId, limit = limit, offset = offset)
            .map { it.toDomainEntity() }
    override suspend fun getMessageCount(chatId: String) = dao.getMessageCountByChatId(chatId)
    override suspend fun upsertMessagesByMessageId(messages: List<MessageEntity>) =
        dao.upsertMessagesByMessageId(messages.map { it.toLocalEntity() })
    override suspend fun clearMessages(chatId: String) = dao.deleteMessagesByChatId(chatId)
    override suspend fun deleteMessageByLocalId(localId: Int) = dao.deleteMessageByLocalId(localId)
    override suspend fun deleteMessageByMessageId(messageId: String) = dao.deleteMessageByMessageId(messageId)
    override suspend fun markMessageSeenByMessageId(messageId: String) = dao.markMessageSeenByMessageId(messageId)
    override suspend fun updateMessageReactionsByMessageId(messageId: String, reactionsJson: String?) =
        dao.updateReactionsByMessageId(messageId, reactionsJson)
    override suspend fun reactToMessage(messageId: String, emoji: String?) =
        chatApiService.reactToMessage(messageId, emoji)

    override suspend fun getUnread(): Resource<UnreadResponse> =
        chatApiService.getUnread()
}
