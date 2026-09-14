package com.dino.message.corefeature.data.db

import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL
import com.dino.message.chatfeature.data.local.MessageLocalEntity
import com.dino.message.chatfeature.data.local.toDomainEntity
import com.dino.message.chatfeature.data.local.toLocalEntity
import com.dino.message.corefeature.data.socket.RoomSocketOutboundQueueStore
import com.dino.message.corefeature.data.socket.SocketOutboundQueueRecord
import com.dino.message.crashfeature.data.local.CrashLogEntity
import com.dino.message.groupfeature.data.local.GroupMessageLocalEntity
import com.dino.message.groupfeature.data.local.toDomainEntity
import com.dino.message.groupfeature.data.local.toLocalEntity
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlinx.coroutines.test.runTest

class MessageDatabasePersistenceTest {

    @Test
    fun migrationFromVersion12_preservesRowsAndAddsReactionColumns() = runTest {
        val path = Files.createTempFile("message-database-v12-", ".db")
        createVersion12Database(path.toString())

        val database = buildMessageDatabase(path.toString())
        try {
            val privateMessage = database.messageDao().getMessageByMessageId("private-v12")
            val groupMessage = database.groupMessageDao().getMessageByMessageId("group-v12")
            assertNotNull(privateMessage)
            assertNotNull(groupMessage)
            assertEquals("private content", privateMessage.content)
            assertEquals("group content", groupMessage.content)
            assertNull(privateMessage.reactionsJson)
            assertNull(groupMessage.reactionsJson)
            assertEquals("queue-v12", database.socketOutboundQueueDao().getAll().single().queueId)
            assertEquals("crash-v12", database.crashLogDao().getOldestPending(1).single().eventId)
        } finally {
            database.close()
            deleteDatabaseFiles(path)
        }
    }

    @Test
    fun privateMessages_preservePagingUpsertAndDomainMapping() = runTest {
        withDatabase { database ->
            val dao = database.messageDao()
            dao.insertMessage(privateMessage(messageId = "private-1", content = "original", timestamp = "1"))
            dao.insertMessage(privateMessage(messageId = "private-2", content = "newest", timestamp = "2"))

            dao.upsertMessagesByMessageId(
                listOf(
                    privateMessage(
                        messageId = "private-1",
                        content = "updated",
                        timestamp = "3",
                        isSeen = true,
                        reactionsJson = "{\"👍\":[\"alice\"]}",
                    )
                )
            )

            assertEquals(2, dao.getMessageCountByChatId("chat-a"))
            val firstPage = dao.getMessagesByChatId("chat-a", limit = 1, offset = 0)
            val secondPage = dao.getMessagesByChatId("chat-a", limit = 1, offset = 1)
            assertEquals("private-1", firstPage.single().messageId)
            assertEquals("private-2", secondPage.single().messageId)

            val mapped = firstPage.single().toDomainEntity().toLocalEntity()
            assertEquals(firstPage.single(), mapped)
            assertEquals("updated", mapped.content)
            assertEquals(true, mapped.isSeen)
        }
    }

    @Test
    fun groupMessages_preservePagingUpsertAndDomainMapping() = runTest {
        withDatabase { database ->
            val dao = database.groupMessageDao()
            dao.insertMessage(groupMessage(messageId = "group-1", content = "original", timestamp = "1"))
            dao.insertMessage(groupMessage(messageId = "group-2", content = "newest", timestamp = "2"))

            dao.upsertMessagesByMessageId(
                listOf(groupMessage(messageId = "group-1", content = "updated", timestamp = "3", isSeen = true))
            )

            assertEquals(2, dao.getMessageCountByGroupId(7))
            val firstPage = dao.getMessagesByGroupId(7, limit = 1, offset = 0)
            val secondPage = dao.getMessagesByGroupId(7, limit = 1, offset = 1)
            assertEquals("group-1", firstPage.single().messageId)
            assertEquals("group-2", secondPage.single().messageId)
            assertEquals(firstPage.single(), firstPage.single().toDomainEntity().toLocalEntity())
        }
    }

    @Test
    fun socketOutboundQueue_preservesOrderingDedupeRetryAndClear() = runTest {
        withDatabase { database ->
            val store = RoomSocketOutboundQueueStore(database.socketOutboundQueueDao())
            assertEquals(true, store.insert(queueRecord("queue-2", "dedupe-2", nextAttemptAtMs = 20, createdAtMs = 1)))
            assertEquals(true, store.insert(queueRecord("queue-1", "dedupe-1", nextAttemptAtMs = 10, createdAtMs = 2)))
            assertFalse(store.insert(queueRecord("queue-3", "dedupe-1", nextAttemptAtMs = 5, createdAtMs = 3)))
            assertEquals(listOf("queue-1", "queue-2"), store.getAll().map { it.queueId })

            store.updateRetryState("queue-1", attempts = 3, nextAttemptAtMs = 30)
            assertEquals(3, store.getAll().first { it.queueId == "queue-1" }.attempts)
            store.deleteByQueueId("queue-2")
            assertEquals(listOf("queue-1"), store.getAll().map { it.queueId })
            store.clearAll()
            assertEquals(emptyList(), store.getAll())
        }
    }

    @Test
    fun crashLogQueue_preservesUniquenessOrderingFailureAndDeletion() = runTest {
        withDatabase { database ->
            val dao = database.crashLogDao()
            assertEquals(1L, dao.insert(crashLog(eventId = "event-2", createdAt = 20)))
            assertEquals(2L, dao.insert(crashLog(eventId = "event-1", createdAt = 10)))
            assertEquals(-1L, dao.insert(crashLog(eventId = "event-1", createdAt = 30)))

            val pending = dao.getOldestPending(limit = 2)
            assertEquals(listOf("event-1", "event-2"), pending.map { it.eventId })
            dao.markFailed(listOf(pending.first().id), "offline")
            val failed = dao.getOldestPending(limit = 1).single()
            assertEquals(1, failed.sendAttempts)
            assertEquals("offline", failed.lastError)
            dao.deleteByIds(listOf(failed.id))
            assertEquals("event-2", dao.getOldestPending(limit = 1).single().eventId)
        }
    }

    @Test
    fun privateMessageNullableIds_keepLegacyInsertBehavior() = runTest {
        withDatabase { database ->
            val dao = database.messageDao()
            dao.insertMessage(privateMessage(messageId = null, content = "first", timestamp = "1"))
            dao.insertMessage(privateMessage(messageId = null, content = "second", timestamp = "2"))
            assertEquals(2, dao.getMessageCountByChatId("chat-a"))
            assertNotNull(dao.getMessagesByChatId("chat-a", 1, 0).single())
            assertNull(dao.getMessageByMessageId("missing"))
        }
    }

    private suspend fun withDatabase(block: suspend (MessageDatabase) -> Unit) {
        val path = Files.createTempFile("message-database-", ".db")
        val database = buildMessageDatabase(path.toString())
        try {
            block(database)
        } finally {
            database.close()
            deleteDatabaseFiles(path)
        }
    }

    private fun createVersion12Database(databasePath: String) {
        BundledSQLiteDriver().open(databasePath).use { connection ->
            VERSION_12_SCHEMA.forEach(connection::execSQL)
            connection.execSQL(
                """
                INSERT INTO messages (
                    messageId, chatId, senderOrReceiver, content, timestamp, messageType, isSeen
                ) VALUES ('private-v12', 'chat-v12', 'alice', 'private content', '1', 'text', 0)
                """.trimIndent()
            )
            connection.execSQL(
                """
                INSERT INTO group_messages (
                    messageId, groupId, sender, content, timestamp, messageType, isSeen
                ) VALUES ('group-v12', 7, 'alice', 'group content', '1', 'text', 0)
                """.trimIndent()
            )
            connection.execSQL(
                """
                INSERT INTO socket_outbound_queue (
                    queueId, event, ownerUsername, payloadType, attempts, nextAttemptAtMs, createdAtMs
                ) VALUES ('queue-v12', 'send_message', 'alice', 'private', 0, 10, 1)
                """.trimIndent()
            )
            connection.execSQL(
                """
                INSERT INTO crash_logs_queue (
                    eventId, appVersion, appVersionCode, threadName, exceptionType, stackTrace,
                    occurredAtEpochMillis, createdAtEpochMillis, deviceModel, deviceManufacturer,
                    osVersion, sdkInt, buildType, sendAttempts
                ) VALUES (
                    'crash-v12', '1', 1, 'main', 'TestException', 'stack',
                    1, 1, 'desktop', 'test', 'test', 0, 'test', 0
                )
                """.trimIndent()
            )
            connection.execSQL("PRAGMA user_version = 12")
        }
    }

    private fun deleteDatabaseFiles(path: java.nio.file.Path) {
        Files.deleteIfExists(path)
        Files.deleteIfExists(path.resolveSibling("${path.fileName}-shm"))
        Files.deleteIfExists(path.resolveSibling("${path.fileName}-wal"))
    }

    private fun privateMessage(
        messageId: String?,
        content: String,
        timestamp: String,
        isSeen: Boolean = false,
        reactionsJson: String? = null,
    ) = MessageLocalEntity(
        messageId = messageId,
        chatId = "chat-a",
        senderOrReceiver = "alice",
        content = content,
        timestamp = timestamp,
        isSeen = isSeen,
        reactionsJson = reactionsJson,
    )

    private fun groupMessage(
        messageId: String,
        content: String,
        timestamp: String,
        isSeen: Boolean = false,
    ) = GroupMessageLocalEntity(
        messageId = messageId,
        groupId = 7,
        sender = "alice",
        content = content,
        timestamp = timestamp,
        isSeen = isSeen,
    )

    private fun queueRecord(
        queueId: String,
        dedupeKey: String,
        nextAttemptAtMs: Long,
        createdAtMs: Long,
    ) = SocketOutboundQueueRecord(
        queueId = queueId,
        event = "send_message",
        ownerUsername = "alice",
        payloadType = "private",
        payloadBody = "{}",
        dedupeKey = dedupeKey,
        attempts = 0,
        nextAttemptAtMs = nextAttemptAtMs,
        createdAtMs = createdAtMs,
    )

    private fun crashLog(eventId: String, createdAt: Long) = CrashLogEntity(
        eventId = eventId,
        appVersion = "1",
        appVersionCode = 1,
        threadName = "main",
        exceptionType = "TestException",
        exceptionMessage = null,
        stackTrace = "stack",
        occurredAtEpochMillis = createdAt,
        createdAtEpochMillis = createdAt,
        deviceModel = "desktop",
        deviceManufacturer = "test",
        osVersion = "test",
        sdkInt = 0,
        buildType = "test",
        username = null,
    )

    private companion object {
        val VERSION_12_SCHEMA = listOf(
            """
            CREATE TABLE messages (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                messageId TEXT,
                chatId TEXT NOT NULL,
                senderOrReceiver TEXT NOT NULL,
                content TEXT NOT NULL,
                timestamp TEXT NOT NULL,
                messageType TEXT NOT NULL,
                attachmentType TEXT,
                attachmentUrl TEXT,
                attachmentMimeType TEXT,
                attachmentFileName TEXT,
                attachmentSizeBytes INTEGER,
                isSeen INTEGER NOT NULL,
                replyToMessageId TEXT,
                replyToSender TEXT,
                replyPreview TEXT
            )
            """.trimIndent(),
            "CREATE UNIQUE INDEX index_messages_messageId ON messages (messageId)",
            "CREATE INDEX index_messages_chatId ON messages (chatId)",
            "CREATE INDEX index_messages_chatId_timestamp_id ON messages (chatId, timestamp, id)",
            """
            CREATE TABLE group_messages (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                messageId TEXT NOT NULL,
                groupId INTEGER NOT NULL,
                sender TEXT NOT NULL,
                content TEXT NOT NULL,
                timestamp TEXT NOT NULL,
                messageType TEXT NOT NULL,
                attachmentType TEXT,
                attachmentUrl TEXT,
                attachmentMimeType TEXT,
                attachmentFileName TEXT,
                attachmentSizeBytes INTEGER,
                isSeen INTEGER NOT NULL,
                replyToMessageId TEXT,
                replyToSender TEXT,
                replyPreview TEXT
            )
            """.trimIndent(),
            "CREATE UNIQUE INDEX index_group_messages_messageId ON group_messages (messageId)",
            "CREATE INDEX index_group_messages_groupId ON group_messages (groupId)",
            "CREATE INDEX index_group_messages_groupId_timestamp_id ON group_messages (groupId, timestamp, id)",
            """
            CREATE TABLE crash_logs_queue (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                eventId TEXT NOT NULL,
                appVersion TEXT NOT NULL,
                appVersionCode INTEGER NOT NULL,
                threadName TEXT NOT NULL,
                exceptionType TEXT NOT NULL,
                exceptionMessage TEXT,
                stackTrace TEXT NOT NULL,
                occurredAtEpochMillis INTEGER NOT NULL,
                createdAtEpochMillis INTEGER NOT NULL,
                deviceModel TEXT NOT NULL,
                deviceManufacturer TEXT NOT NULL,
                osVersion TEXT NOT NULL,
                sdkInt INTEGER NOT NULL,
                buildType TEXT NOT NULL,
                username TEXT,
                sendAttempts INTEGER NOT NULL,
                lastError TEXT
            )
            """.trimIndent(),
            "CREATE UNIQUE INDEX index_crash_logs_queue_eventId ON crash_logs_queue (eventId)",
            "CREATE INDEX index_crash_logs_queue_createdAtEpochMillis ON crash_logs_queue (createdAtEpochMillis)",
            """
            CREATE TABLE socket_outbound_queue (
                queueId TEXT NOT NULL,
                event TEXT NOT NULL,
                ownerUsername TEXT NOT NULL,
                payloadType TEXT NOT NULL,
                payloadBody TEXT,
                dedupeKey TEXT,
                attempts INTEGER NOT NULL,
                nextAttemptAtMs INTEGER NOT NULL,
                createdAtMs INTEGER NOT NULL,
                PRIMARY KEY(queueId)
            )
            """.trimIndent(),
            "CREATE INDEX index_socket_outbound_queue_nextAttemptAtMs ON socket_outbound_queue (nextAttemptAtMs)",
            "CREATE UNIQUE INDEX index_socket_outbound_queue_dedupeKey ON socket_outbound_queue (dedupeKey)",
        )
    }
}
