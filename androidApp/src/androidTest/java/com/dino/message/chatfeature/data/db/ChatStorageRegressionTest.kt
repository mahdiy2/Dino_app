package com.dino.message.chatfeature.data.db

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.dino.message.chatfeature.data.local.MessageLocalEntity
import com.dino.message.corefeature.data.db.MessageDatabase
import com.dino.message.groupfeature.data.db.GroupMessageDao
import com.dino.message.groupfeature.data.local.GroupMessageLocalEntity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant

@RunWith(AndroidJUnit4::class)
class ChatStorageRegressionTest {

    private lateinit var database: MessageDatabase
    private lateinit var messageDao: MessageDao
    private lateinit var groupMessageDao: GroupMessageDao

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        database = Room.inMemoryDatabaseBuilder(context, MessageDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        messageDao = database.messageDao()
        groupMessageDao = database.groupMessageDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun privateMessageQuery_isPaginatedAndNewestFirst() = runBlocking {
        for (index in 0 until 160) {
            messageDao.insertMessage(
                MessageLocalEntity(
                    messageId = "private-$index",
                    chatId = "chat-a",
                    senderOrReceiver = "user-a",
                    content = "message-$index",
                    timestamp = Instant.ofEpochSecond(index.toLong()).toString(),
                )
            )
        }
        for (index in 0 until 10) {
            messageDao.insertMessage(
                MessageLocalEntity(
                    messageId = "other-$index",
                    chatId = "chat-b",
                    senderOrReceiver = "user-b",
                    content = "other-$index",
                    timestamp = Instant.ofEpochSecond(index.toLong()).toString(),
                )
            )
        }

        val firstPage = messageDao.getMessagesByChatId(chatId = "chat-a", limit = 50, offset = 0)
        val secondPage = messageDao.getMessagesByChatId(chatId = "chat-a", limit = 50, offset = 50)

        assertEquals(50, firstPage.size)
        assertEquals(50, secondPage.size)
        assertEquals("private-159", firstPage.first().messageId)
        assertEquals("private-110", firstPage.last().messageId)
        assertEquals("private-109", secondPage.first().messageId)
        assertEquals("private-60", secondPage.last().messageId)
        assertTrue(firstPage.all { it.chatId == "chat-a" })
        assertTrue(secondPage.all { it.chatId == "chat-a" })
    }

    @Test
    fun groupMessageQuery_isPaginatedAndNewestFirst() = runBlocking {
        for (index in 0 until 140) {
            groupMessageDao.insertMessage(
                GroupMessageLocalEntity(
                    messageId = "group-$index",
                    groupId = 7,
                    sender = "sender-a",
                    content = "group-message-$index",
                    timestamp = Instant.ofEpochSecond(index.toLong()).toString(),
                )
            )
        }
        for (index in 0 until 8) {
            groupMessageDao.insertMessage(
                GroupMessageLocalEntity(
                    messageId = "group-other-$index",
                    groupId = 9,
                    sender = "sender-b",
                    content = "other-group-message-$index",
                    timestamp = Instant.ofEpochSecond(index.toLong()).toString(),
                )
            )
        }

        val firstPage = groupMessageDao.getMessagesByGroupId(groupId = 7, limit = 40, offset = 0)
        val secondPage = groupMessageDao.getMessagesByGroupId(groupId = 7, limit = 40, offset = 40)

        assertEquals(40, firstPage.size)
        assertEquals(40, secondPage.size)
        assertEquals("group-139", firstPage.first().messageId)
        assertEquals("group-100", firstPage.last().messageId)
        assertEquals("group-99", secondPage.first().messageId)
        assertEquals("group-60", secondPage.last().messageId)
        assertTrue(firstPage.all { it.groupId == 7 })
        assertTrue(secondPage.all { it.groupId == 7 })
    }

    @Test
    fun messageTables_keepCompositeIndexesForHistoryQueries() {
        assertIndexExists(tableName = "messages", expectedIndex = "index_messages_chatId_timestamp_id")
        assertIndexExists(tableName = "group_messages", expectedIndex = "index_group_messages_groupId_timestamp_id")
    }

    private fun assertIndexExists(tableName: String, expectedIndex: String) {
        val query = "PRAGMA index_list('$tableName')"
        database.openHelper.readableDatabase.query(query).use { cursor ->
            val nameColumn = cursor.getColumnIndexOrThrow("name")
            var found = false
            while (cursor.moveToNext()) {
                if (cursor.getString(nameColumn) == expectedIndex) {
                    found = true
                    break
                }
            }
            assertTrue("Expected index $expectedIndex on $tableName", found)
        }
    }
}
