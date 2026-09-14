package com.dino.message.persistence

import com.dino.message.chatfeature.data.db.MessageDao
import com.dino.message.chatfeature.data.local.MessageLocalEntity
import com.dino.message.chatfeature.data.remote.ChatApiService
import com.dino.message.chatfeature.data.repository.ChatRepositoryImpl
import com.dino.message.chatfeature.domain.model.MessageEntity
import com.dino.message.groupfeature.data.db.GroupMessageDao
import com.dino.message.groupfeature.data.local.GroupMessageLocalEntity
import com.dino.message.groupfeature.data.remote.GroupApiService
import com.dino.message.groupfeature.data.repository.GroupRepositoryImpl
import com.dino.message.groupfeature.domain.model.GroupMessageEntity
import java.lang.reflect.Proxy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PersistenceRepositoryTest {

    @Test
    fun chatRepository_mapsDomainMessagesAndDelegatesPersistenceOperations() = runTest {
        val dao = FakeMessageDao()
        val repository = ChatRepositoryImpl(unusedService(), dao)
        val message = MessageEntity(
            messageId = "private-1",
            chatId = "chat-a",
            senderOrReceiver = "alice",
            content = "hello",
            timestamp = "1",
        )

        repository.saveMessage(message)
        assertEquals(message.copy(id = 1), repository.getMessages("chat-a", limit = 20, offset = 0).single())

        repository.upsertMessagesByMessageId(
            listOf(message.copy(content = "updated", timestamp = "2", isSeen = true))
        )
        assertEquals(1, repository.getMessageCount("chat-a"))
        assertEquals("updated", repository.getMessages("chat-a", 20, 0).single().content)

        repository.updateMessageReactionsByMessageId("private-1", "{\"👍\":[\"alice\"]}")
        assertTrue(repository.getMessages("chat-a", 20, 0).single().reactionsJson?.contains("alice") == true)

        repository.clearMessages("chat-a")
        assertEquals(0, repository.getMessageCount("chat-a"))
    }

    @Test
    fun groupRepository_mapsDomainMessagesAndDelegatesPersistenceOperations() = runTest {
        val dao = FakeGroupMessageDao()
        val repository = GroupRepositoryImpl(unusedService(), dao)
        val message = GroupMessageEntity(
            messageId = "group-1",
            groupId = 7,
            sender = "alice",
            content = "hello group",
            timestamp = "1",
        )

        repository.saveGroupMessage(message)
        assertEquals(
            message.copy(id = 1),
            repository.getGroupMessagesFromDb(groupId = 7, limit = 20, offset = 0).single(),
        )

        repository.upsertGroupMessagesByMessageId(
            listOf(message.copy(content = "updated", timestamp = "2", isSeen = true))
        )
        assertEquals(1, repository.getGroupMessageCount(7))
        assertEquals("updated", repository.getGroupMessagesFromDb(7, 20, 0).single().content)

        repository.updateGroupMessageReactionsByMessageId("group-1", "{\"🔥\":[\"alice\"]}")
        assertTrue(repository.getGroupMessagesFromDb(7, 20, 0).single().reactionsJson?.contains("alice") == true)

        repository.clearGroupMessages(7)
        assertEquals(0, repository.getGroupMessageCount(7))
    }

    private class FakeMessageDao : MessageDao {
        private val messages = mutableListOf<MessageLocalEntity>()
        private var nextId = 1

        override suspend fun insertMessage(message: MessageLocalEntity) {
            if (message.messageId != null && messages.any { it.messageId == message.messageId }) return
            messages += message.copy(id = if (message.id == 0) nextId++ else message.id)
        }

        override suspend fun getMessagesByChatId(chatId: String, limit: Int, offset: Int) = messages
            .filter { it.chatId == chatId }
            .sortedWith(compareByDescending<MessageLocalEntity> { it.timestamp }.thenByDescending { it.id })
            .drop(offset)
            .take(limit)

        override suspend fun getMessageCountByChatId(chatId: String) = messages.count { it.chatId == chatId }

        override suspend fun deleteMessagesByChatId(chatId: String) {
            messages.removeAll { it.chatId == chatId }
        }

        override suspend fun deleteMessageByLocalId(localId: Int) {
            messages.removeAll { it.id == localId }
        }

        override suspend fun deleteMessageByMessageId(messageId: String) {
            messages.removeAll { it.messageId == messageId }
        }

        override suspend fun markMessageSeenByMessageId(messageId: String) {
            replace(messageId) { it.copy(isSeen = true) }
        }

        override suspend fun updateReactionsByMessageId(messageId: String, reactionsJson: String?) {
            replace(messageId) { it.copy(reactionsJson = reactionsJson) }
        }

        override suspend fun getAllMessages() = messages.toList()

        override suspend fun deleteAllMessages() {
            messages.clear()
        }

        override suspend fun getMessageByMessageId(messageId: String) = messages.firstOrNull { it.messageId == messageId }

        override suspend fun updateMessage(message: MessageLocalEntity) {
            val index = messages.indexOfFirst { it.id == message.id }
            if (index >= 0) messages[index] = message
        }

        private fun replace(messageId: String, transform: (MessageLocalEntity) -> MessageLocalEntity) {
            val index = messages.indexOfFirst { it.messageId == messageId }
            if (index >= 0) messages[index] = transform(messages[index])
        }
    }

    private class FakeGroupMessageDao : GroupMessageDao {
        private val messages = mutableListOf<GroupMessageLocalEntity>()
        private var nextId = 1

        override suspend fun insertMessage(message: GroupMessageLocalEntity) {
            if (messages.any { it.messageId == message.messageId }) return
            messages += message.copy(id = if (message.id == 0) nextId++ else message.id)
        }

        override suspend fun getMessagesByGroupId(groupId: Int, limit: Int, offset: Int) = messages
            .filter { it.groupId == groupId }
            .sortedWith(compareByDescending<GroupMessageLocalEntity> { it.timestamp }.thenByDescending { it.id })
            .drop(offset)
            .take(limit)

        override suspend fun getMessageCountByGroupId(groupId: Int) = messages.count { it.groupId == groupId }

        override suspend fun deleteMessagesByGroupId(groupId: Int) {
            messages.removeAll { it.groupId == groupId }
        }

        override suspend fun deleteMessageByMessageId(messageId: String) {
            messages.removeAll { it.messageId == messageId }
        }

        override suspend fun markMessageSeenByMessageId(messageId: String) {
            replace(messageId) { it.copy(isSeen = true) }
        }

        override suspend fun updateReactionsByMessageId(messageId: String, reactionsJson: String?) {
            replace(messageId) { it.copy(reactionsJson = reactionsJson) }
        }

        override suspend fun getMessageByMessageId(messageId: String) = messages.firstOrNull { it.messageId == messageId }

        override suspend fun updateMessage(message: GroupMessageLocalEntity) {
            val index = messages.indexOfFirst { it.id == message.id }
            if (index >= 0) messages[index] = message
        }

        private fun replace(messageId: String, transform: (GroupMessageLocalEntity) -> GroupMessageLocalEntity) {
            val index = messages.indexOfFirst { it.messageId == messageId }
            if (index >= 0) messages[index] = transform(messages[index])
        }
    }

    private inline fun <reified T> unusedService(): T {
        return Proxy.newProxyInstance(
            T::class.java.classLoader,
            arrayOf(T::class.java),
        ) { _, method, _ ->
            when (method.name) {
                "toString" -> "Unused${T::class.java.simpleName}"
                "hashCode" -> 0
                "equals" -> false
                else -> error("Unexpected service call: ${method.name}")
            }
        } as T
    }
}
