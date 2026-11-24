package com.dino.message.corefeature.data.socket

import kotlinx.coroutines.test.runTest
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class IndexedDbSocketOutboundQueueStoreTest {
    @Test
    fun persistsRecordsAndRetryStateAcrossInstances() = runTest {
        val name = "dino-realtime-test-${Random.nextLong()}"
        val first = IndexedDbSocketOutboundQueueStore(name)
        val row = SocketOutboundQueueRecord("1", "send_message", "alice", "string", "body", "key", 0, 1, 1)
        assertTrue(first.insert(row))
        val reopened = IndexedDbSocketOutboundQueueStore(name)
        assertFalse(reopened.insert(row.copy(queueId = "2")))
        reopened.updateRetryState("1", 2, 9000)
        assertEquals(listOf(row.copy(attempts = 2, nextAttemptAtMs = 9000)), first.getAll())
        first.clearAll()
        assertTrue(reopened.getAll().isEmpty())
    }
}
