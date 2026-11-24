package com.dino.message.corefeature.data.socket

import com.dino.message.corefeature.data.db.buildMessageDatabase
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.test.runTest
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSUUID
import platform.posix.unlink
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalForeignApi::class)
class IosSocketOutboundQueueStoreTest {
    @Test
    fun reopensQueuedEventsWithPersistedRetryState() = runTest {
        val path = "${NSTemporaryDirectory()}/realtime-${NSUUID().UUIDString}.db"
        val record = SocketOutboundQueueRecord(
            queueId = "queue-1",
            event = "send_message",
            ownerUsername = "alice",
            payloadType = "json_object",
            payloadBody = """{"client_message_id":"message-1"}""",
            dedupeKey = "message-1",
            attempts = 0,
            nextAttemptAtMs = 10,
            createdAtMs = 1,
        )
        try {
            val original = buildMessageDatabase(path)
            try {
                val store = RoomSocketOutboundQueueStore(original.socketOutboundQueueDao())
                assertTrue(store.insert(record))
                assertFalse(store.insert(record.copy(queueId = "queue-2")))
                store.updateRetryState(record.queueId, attempts = 2, nextAttemptAtMs = 4_000)
            } finally {
                original.close()
            }

            val reopened = buildMessageDatabase(path)
            try {
                val store = RoomSocketOutboundQueueStore(reopened.socketOutboundQueueDao())
                assertEquals(listOf(record.copy(attempts = 2, nextAttemptAtMs = 4_000)), store.getAll())
                store.deleteByQueueId(record.queueId)
                assertTrue(store.getAll().isEmpty())
            } finally {
                reopened.close()
            }
        } finally {
            for (suffix in listOf("", "-wal", "-shm", "-journal")) {
                unlink(path + suffix)
            }
        }
    }
}
