package com.dino.message.corefeature.data.socket

import kotlin.math.max
import kotlin.math.min

class SocketOutboundRetryQueue(
    private val maxRetries: Int,
    private val initialBackoffMs: Long,
    private val maxBackoffMs: Long,
    private val maxQueueSize: Int,
    private val timeProvider: () -> Long,
    private val queueIdProvider: () -> String,
) {

    init {
        require(maxRetries >= 0) { "maxRetries must be >= 0" }
        require(initialBackoffMs > 0) { "initialBackoffMs must be > 0" }
        require(maxBackoffMs >= initialBackoffMs) { "maxBackoffMs must be >= initialBackoffMs" }
        require(maxQueueSize > 0) { "maxQueueSize must be > 0" }
    }

    data class QueueItem(
        val queueId: String,
        val event: String,
        val payload: Any?,
        val dedupeKey: String?,
        val attempts: Int,
        val nextAttemptAtMs: Long,
    )

    sealed class EnqueueResult {
        data class Queued(val item: QueueItem) : EnqueueResult()
        object AlreadyQueued : EnqueueResult()
        object QueueFull : EnqueueResult()
    }

    sealed class FailureResult {
        data class RetryScheduled(
            val item: QueueItem,
            val delayMs: Long,
        ) : FailureResult()

        data class Dropped(
            val item: QueueItem,
        ) : FailureResult()
    }

    private val items = mutableListOf<QueueItem>()
    private val dedupeKeys = mutableSetOf<String>()

    val size: Int
        get() = items.size

    fun clear() {
        items.clear()
        dedupeKeys.clear()
    }

    fun replaceAll(restoredItems: List<QueueItem>) {
        items.clear()
        dedupeKeys.clear()
        if (restoredItems.isEmpty()) {
            return
        }
        items.addAll(restoredItems)
        sortByRetryAt()
        restoredItems.forEach { item ->
            item.dedupeKey?.let { dedupeKeys.add(it) }
        }
    }

    fun enqueue(
        event: String,
        payload: Any?,
        dedupeKey: String?,
    ): EnqueueResult {
        val normalizedKey = dedupeKey?.trim()?.takeIf { it.isNotBlank() }
        if (normalizedKey != null && dedupeKeys.contains(normalizedKey)) {
            return EnqueueResult.AlreadyQueued
        }
        if (items.size >= maxQueueSize) {
            return EnqueueResult.QueueFull
        }

        val queued = QueueItem(
            queueId = queueIdProvider(),
            event = event,
            payload = payload,
            dedupeKey = normalizedKey,
            attempts = 0,
            nextAttemptAtMs = timeProvider(),
        )
        items.add(queued)
        sortByRetryAt()
        if (normalizedKey != null) {
            dedupeKeys.add(normalizedKey)
        }
        return EnqueueResult.Queued(queued)
    }

    fun peekReady(nowMs: Long = timeProvider()): QueueItem? {
        val head = items.firstOrNull() ?: return null
        return if (head.nextAttemptAtMs <= nowMs) head else null
    }

    fun millisUntilNextReady(nowMs: Long = timeProvider()): Long? {
        val head = items.firstOrNull() ?: return null
        return max(0L, head.nextAttemptAtMs - nowMs)
    }

    fun markSuccess(queueId: String): QueueItem? {
        val index = items.indexOfFirst { it.queueId == queueId }
        if (index < 0) return null
        return removeAt(index)
    }

    fun markFailure(queueId: String): FailureResult? {
        val index = items.indexOfFirst { it.queueId == queueId }
        if (index < 0) return null

        val current = removeAt(index)
        val nextAttempt = current.attempts + 1
        if (nextAttempt > maxRetries) {
            return FailureResult.Dropped(current.copy(attempts = nextAttempt))
        }

        val delayMs = computeBackoffDelay(nextAttempt)
        val retryItem = current.copy(
            attempts = nextAttempt,
            nextAttemptAtMs = timeProvider() + delayMs,
        )
        items.add(retryItem)
        sortByRetryAt()
        retryItem.dedupeKey?.let { dedupeKeys.add(it) }
        return FailureResult.RetryScheduled(retryItem, delayMs)
    }

    private fun removeAt(index: Int): QueueItem {
        val removed = items.removeAt(index)
        removed.dedupeKey?.let { dedupeKeys.remove(it) }
        return removed
    }

    private fun sortByRetryAt() {
        items.sortBy { it.nextAttemptAtMs }
    }

    private fun computeBackoffDelay(attempt: Int): Long {
        var delay = initialBackoffMs
        repeat(attempt - 1) {
            delay = min(maxBackoffMs, delay * 2)
        }
        return delay
    }
}
