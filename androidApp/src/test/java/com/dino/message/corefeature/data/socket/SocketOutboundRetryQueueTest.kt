package com.dino.message.corefeature.data.socket

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class SocketOutboundRetryQueueTest {

    @Test
    fun enqueueWithSameDedupeKey_onlyQueuesOnce() {
        val queue = SocketOutboundRetryQueue(
            maxRetries = 3,
            initialBackoffMs = 1_000,
            maxBackoffMs = 8_000,
            maxQueueSize = 10,
            timeProvider = { 100L },
        )

        val first = queue.enqueue("send_group_message", payload = "a", dedupeKey = "msg-1")
        val second = queue.enqueue("send_group_message", payload = "a", dedupeKey = "msg-1")

        assertTrue(first is SocketOutboundRetryQueue.EnqueueResult.Queued)
        assertEquals(SocketOutboundRetryQueue.EnqueueResult.AlreadyQueued, second)
        assertEquals(1, queue.size)
    }

    @Test
    fun markFailure_appliesBackoffAndDropsAfterMaxRetries() {
        var now = 0L
        val queue = SocketOutboundRetryQueue(
            maxRetries = 2,
            initialBackoffMs = 1_000,
            maxBackoffMs = 10_000,
            maxQueueSize = 10,
            timeProvider = { now },
        )

        val queued = queue.enqueue("send_message", payload = "hello", dedupeKey = "msg-2")
        val queuedItem = (queued as SocketOutboundRetryQueue.EnqueueResult.Queued).item

        assertNotNull(queue.peekReady(now))

        val firstFailure = queue.markFailure(queuedItem.queueId)
        assertTrue(firstFailure is SocketOutboundRetryQueue.FailureResult.RetryScheduled)
        val firstRetry = firstFailure as SocketOutboundRetryQueue.FailureResult.RetryScheduled
        assertEquals(1_000, firstRetry.delayMs)
        assertNull(queue.peekReady(now))

        now += 1_000
        assertNotNull(queue.peekReady(now))

        val secondFailure = queue.markFailure(queuedItem.queueId)
        assertTrue(secondFailure is SocketOutboundRetryQueue.FailureResult.RetryScheduled)
        val secondRetry = secondFailure as SocketOutboundRetryQueue.FailureResult.RetryScheduled
        assertEquals(2_000, secondRetry.delayMs)

        now += 2_000
        assertNotNull(queue.peekReady(now))

        val dropped = queue.markFailure(queuedItem.queueId)
        assertTrue(dropped is SocketOutboundRetryQueue.FailureResult.Dropped)
        assertEquals(0, queue.size)
    }

    @Test
    fun enqueue_whenQueueFull_returnsQueueFull() {
        val queue = SocketOutboundRetryQueue(
            maxRetries = 1,
            initialBackoffMs = 500,
            maxBackoffMs = 1_000,
            maxQueueSize = 1,
            timeProvider = { 0L },
        )

        queue.enqueue("send_message", payload = "first", dedupeKey = "m1")
        val full = queue.enqueue("send_message", payload = "second", dedupeKey = "m2")

        assertEquals(SocketOutboundRetryQueue.EnqueueResult.QueueFull, full)
        assertEquals(1, queue.size)
    }

    @Test
    fun replaceAll_restoresDedupeAndOrdering() {
        val queue = SocketOutboundRetryQueue(
            maxRetries = 1,
            initialBackoffMs = 500,
            maxBackoffMs = 1_000,
            maxQueueSize = 10,
            timeProvider = { 0L },
        )

        val restored = listOf(
            SocketOutboundRetryQueue.QueueItem(
                queueId = UUID.randomUUID().toString(),
                event = "send_message",
                payload = "a",
                dedupeKey = "msg-1",
                attempts = 0,
                nextAttemptAtMs = 500L,
            ),
            SocketOutboundRetryQueue.QueueItem(
                queueId = UUID.randomUUID().toString(),
                event = "send_message",
                payload = "b",
                dedupeKey = "msg-2",
                attempts = 1,
                nextAttemptAtMs = 100L,
            ),
        )

        queue.replaceAll(restored)

        val ready = queue.peekReady(nowMs = 100L)
        assertEquals("msg-2", ready?.dedupeKey)

        val duplicate = queue.enqueue(
            event = "send_message",
            payload = "duplicate",
            dedupeKey = "msg-1",
        )
        assertEquals(SocketOutboundRetryQueue.EnqueueResult.AlreadyQueued, duplicate)
    }
}
