package com.dino.message.corefeature.data.socket

import android.util.Log
import com.dino.message.corefeature.data.spref.SPrefManager
import com.dino.message.crashfeature.data.service.CrashReporter
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull

class AppSocketManager constructor(
    private val sprefManager: SPrefManager,
    private val crashReporter: CrashReporter,
    private val socketOutboundQueueStore: SocketOutboundQueueStore,
    private val transportFactory: RealtimeSocketTransportFactory,
) : RealtimeSocketGateway {

    enum class EmitWithRetryResult {
        EMITTED,
        QUEUED,
        ALREADY_QUEUED,
        QUEUE_FULL,
    }

    companion object {
        private const val TAG = "AppSocketManager"
        private const val DEFAULT_CONNECT_TIMEOUT_MS = 7_000L
        private const val MAX_OUTBOUND_QUEUE_SIZE = 200
        private const val MAX_OUTBOUND_RETRIES = 5
        private const val OUTBOUND_BACKOFF_INITIAL_MS = 1_000L
        private const val OUTBOUND_BACKOFF_MAX_MS = 15_000L
        private const val OUTBOUND_CONNECT_RETRY_IDLE_MS = 1_000L
    }

    private val lock = Any()
    private val listenerBuckets = mutableMapOf<String, MutableMap<String, RealtimeSocketListener>>()
    private val connectWaiters = mutableListOf<CompletableDeferred<Boolean>>()
    private val outboundRetryQueue = SocketOutboundRetryQueue(
        maxRetries = MAX_OUTBOUND_RETRIES,
        initialBackoffMs = OUTBOUND_BACKOFF_INITIAL_MS,
        maxBackoffMs = OUTBOUND_BACKOFF_MAX_MS,
        maxQueueSize = MAX_OUTBOUND_QUEUE_SIZE,
    )
    private val outboundScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var outboundQueueJob: Job? = null
    private val _queuedOutboundCount = MutableStateFlow(0)
    override val queuedOutboundCount: StateFlow<Int> = _queuedOutboundCount.asStateFlow()

    private var socket: RealtimeSocketTransport? = null
    private var authToken: String? = null
    private var isConnecting = false

    init {
        restorePersistedOutboundQueue()
    }

    override fun connect(forceReconnect: Boolean): Boolean {
        val token = sprefManager.getToken()
            .removePrefix("Bearer ")
            .removePrefix("bearer ")
            .trim()
        if (token.isBlank()) {
            Log.w(TAG, "Token is blank; skipping socket connect")
            crashReporter.addBreadcrumb(
                category = "socket",
                message = "Skipping socket connect because auth token is blank"
            )
            return false
        }

        synchronized(lock) {
            val activeSocket = socket
            val canReuseExisting = !forceReconnect && activeSocket != null && authToken == token

            if (canReuseExisting) {
                if (activeSocket?.isConnected() == true) {
                    return true
                }
                if (!isConnecting) {
                    isConnecting = true
                    activeSocket?.connect()
                }
                return true
            }

            socket?.offAll()
            socket?.disconnect()

            val createdSocket = transportFactory.create(token) ?: run {
                completeConnectionWaitersLocked(success = false)
                isConnecting = false
                return false
            }

            socket = createdSocket
            authToken = token
            isConnecting = true

            createdSocket.on(RealtimeSocketEvents.CONNECT, RealtimeSocketListener {
                crashReporter.addBreadcrumb(
                    category = "socket",
                    message = "Socket connected"
                )
                val waiters = synchronized(lock) {
                    isConnecting = false
                    ensureOutboundQueueWorkerLocked(restart = true)
                    drainConnectionWaitersLocked()
                }
                waiters.forEach { waiter ->
                    if (!waiter.isCompleted) waiter.complete(true)
                }
            })

            createdSocket.on(RealtimeSocketEvents.DISCONNECT, RealtimeSocketListener {
                crashReporter.addBreadcrumb(
                    category = "socket",
                    message = "Socket disconnected"
                )
                synchronized(lock) {
                    isConnecting = false
                    completeConnectionWaitersLocked(success = false)
                }
            })

            createdSocket.on(RealtimeSocketEvents.CONNECT_ERROR, RealtimeSocketListener { args ->
                val error = args.firstOrNull()?.toString()?.take(160) ?: "unknown"
                crashReporter.addBreadcrumb(
                    category = "socket",
                    message = "Socket connect error=$error"
                )
                synchronized(lock) {
                    isConnecting = false
                    completeConnectionWaitersLocked(success = false)
                }
            })

            attachListenersLocked(createdSocket)
            createdSocket.connect()
            return true
        }
    }

    override suspend fun awaitConnected(timeoutMs: Long): Boolean {
        if (isConnected()) return true
        if (!connect()) return false

        val waiter = CompletableDeferred<Boolean>()
        synchronized(lock) {
            if (socket?.isConnected() == true) {
                return true
            }
            connectWaiters.add(waiter)
        }

        return withTimeoutOrNull(timeoutMs) {
            waiter.await()
        } ?: false
    }

    override fun disconnect(clearListeners: Boolean) {
        synchronized(lock) {
            socket?.offAll()
            socket?.disconnect()
            socket = null
            authToken = null
            isConnecting = false
            completeConnectionWaitersLocked(success = false)
            if (clearListeners) {
                listenerBuckets.clear()
                outboundRetryQueue.clear()
                syncOutboundQueueCountLocked()
            }
        }
        if (clearListeners) {
            clearPersistedOutboundQueue()
        }
    }

    override fun isConnected(): Boolean = synchronized(lock) {
        socket?.isConnected() == true
    }

    override fun registerListener(
        owner: String,
        event: String,
        listener: (Array<out Any>) -> Unit,
    ) {
        val wrappedListener = RealtimeSocketListener { args -> listener(args) }

        synchronized(lock) {
            val ownerListeners = listenerBuckets.getOrPut(owner) { mutableMapOf() }
            ownerListeners.remove(event)?.let { previous ->
                socket?.off(event, previous)
            }
            ownerListeners[event] = wrappedListener
            socket?.on(event, wrappedListener)
        }
    }

    override fun unregisterOwner(owner: String) {
        synchronized(lock) {
            val listeners = listenerBuckets.remove(owner) ?: return
            listeners.forEach { (event, listener) ->
                socket?.off(event, listener)
            }
        }
    }

    override fun emit(event: String, payload: Any?): Boolean {
        if (SocketRetryPolicy.requiresRetry(event)) {
            val dedupeKey = SocketRetryPolicy.autoDedupeKey(event, payload)
            crashReporter.addBreadcrumb(
                category = "socket",
                message = "Direct emit rerouted to retry queue for event=$event"
            )
            return when (emitWithRetry(event = event, payload = payload, dedupeKey = dedupeKey)) {
                EmitWithRetryResult.QUEUE_FULL -> false
                EmitWithRetryResult.EMITTED,
                EmitWithRetryResult.QUEUED,
                EmitWithRetryResult.ALREADY_QUEUED -> true
            }
        }
        return emitImmediate(event, payload)
    }

    override fun emitNonRetryable(
        event: String,
        payload: Any?,
        reason: String,
    ): Boolean {
        val normalizedReason = reason.trim().ifBlank { "unspecified" }
        crashReporter.addBreadcrumb(
            category = "socket",
            message = "Non-retryable emit event=$event reason=$normalizedReason"
        )
        return emitImmediate(event, payload)
    }

    private fun emitImmediate(event: String, payload: Any? = null): Boolean {
        val activeSocket = synchronized(lock) { socket }
        if (activeSocket == null || !activeSocket.isConnected()) {
            return false
        }
        activeSocket.emit(event, payload)
        return true
    }

    override fun emitWithRetry(
        event: String,
        payload: Any?,
        dedupeKey: String?,
    ): EmitWithRetryResult {
        if (emitImmediate(event, payload)) {
            return EmitWithRetryResult.EMITTED
        }

        val enqueueResult = synchronized(lock) {
            val result = outboundRetryQueue.enqueue(
                event = event,
                payload = payload,
                dedupeKey = dedupeKey,
            )
            when (result) {
                is SocketOutboundRetryQueue.EnqueueResult.Queued -> {
                    syncOutboundQueueCountLocked()
                    ensureOutboundQueueWorkerLocked(restart = true)
                }
                SocketOutboundRetryQueue.EnqueueResult.AlreadyQueued,
                SocketOutboundRetryQueue.EnqueueResult.QueueFull -> Unit
            }
            result
        }

        val finalResult = when (enqueueResult) {
            is SocketOutboundRetryQueue.EnqueueResult.Queued -> {
                when (persistQueueItem(enqueueResult.item)) {
                    PersistQueueResult.PERSISTED -> Unit
                    PersistQueueResult.DUPLICATE -> {
                        synchronized(lock) {
                            outboundRetryQueue.markSuccess(enqueueResult.item.queueId)
                            syncOutboundQueueCountLocked()
                        }
                        return EmitWithRetryResult.ALREADY_QUEUED
                    }
                    PersistQueueResult.FAILED -> {
                        crashReporter.reportSocketOutboundQueueIssue(
                            issueType = "SocketOutboundQueuePersistenceFailed",
                            event = event,
                            reason = "db_insert_failed",
                            attempts = enqueueResult.item.attempts,
                            dedupeKey = dedupeKey,
                        )
                    }
                }
                Log.w(
                    TAG,
                    "Queued outbound emit event=$event dedupeKey=${dedupeKey ?: enqueueResult.item.queueId}"
                )
                EmitWithRetryResult.QUEUED
            }
            SocketOutboundRetryQueue.EnqueueResult.AlreadyQueued -> EmitWithRetryResult.ALREADY_QUEUED
            SocketOutboundRetryQueue.EnqueueResult.QueueFull -> {
                Log.e(TAG, "Outbound queue full; dropping event=$event")
                crashReporter.reportSocketOutboundQueueIssue(
                    issueType = "SocketOutboundQueueFull",
                    event = event,
                    reason = "queue_full",
                    attempts = 0,
                    dedupeKey = dedupeKey,
                )
                EmitWithRetryResult.QUEUE_FULL
            }
        }

        connect(forceReconnect = false)
        return finalResult
    }

    private fun attachListenersLocked(targetSocket: RealtimeSocketTransport) {
        listenerBuckets.values.forEach { listenerMap ->
            listenerMap.forEach { (event, listener) ->
                targetSocket.on(event, listener)
            }
        }
    }

    private fun completeConnectionWaitersLocked(success: Boolean) {
        val waiters = drainConnectionWaitersLocked()
        waiters.forEach { waiter ->
            if (!waiter.isCompleted) waiter.complete(success)
        }
    }

    private fun drainConnectionWaitersLocked(): List<CompletableDeferred<Boolean>> {
        if (connectWaiters.isEmpty()) return emptyList()
        val pending = connectWaiters.toList()
        connectWaiters.clear()
        return pending
    }

    private fun ensureOutboundQueueWorkerLocked(restart: Boolean) {
        if (restart) {
            outboundQueueJob?.cancel()
            outboundQueueJob = null
        }
        if (outboundQueueJob?.isActive == true) {
            return
        }
        outboundQueueJob = outboundScope.launch {
            processOutboundQueue()
        }
    }

    private suspend fun processOutboundQueue() {
        while (true) {
            val readyItem = synchronized(lock) {
                outboundRetryQueue.peekReady()
            }
            if (readyItem == null) {
                val waitMs = synchronized(lock) {
                    outboundRetryQueue.millisUntilNextReady()
                }
                if (waitMs == null) {
                    return
                }
                delay(waitMs)
                continue
            }

            if (!isConnected()) {
                val connectRequested = connect(forceReconnect = false)
                if (!connectRequested) {
                    delay(OUTBOUND_CONNECT_RETRY_IDLE_MS)
                    continue
                }
                val connected = awaitConnected(DEFAULT_CONNECT_TIMEOUT_MS)
                if (!connected) {
                    handleOutboundFailure(readyItem, "connect timeout")
                    continue
                }
            }

            val emitted = emitImmediate(readyItem.event, readyItem.payload)
            if (emitted) {
                var removedItem: SocketOutboundRetryQueue.QueueItem? = null
                synchronized(lock) {
                    removedItem = outboundRetryQueue.markSuccess(readyItem.queueId)
                    syncOutboundQueueCountLocked()
                }
                removedItem?.let { deletePersistedQueueItem(it.queueId) }
                Log.d(TAG, "Flushed queued outbound event=${readyItem.event}")
                continue
            }

            handleOutboundFailure(readyItem, "socket disconnected before emit")
        }
    }

    private fun handleOutboundFailure(
        item: SocketOutboundRetryQueue.QueueItem,
        reason: String,
    ) {
        val failure = synchronized(lock) {
            val result = outboundRetryQueue.markFailure(item.queueId)
            syncOutboundQueueCountLocked()
            result
        }

        when (failure) {
            is SocketOutboundRetryQueue.FailureResult.RetryScheduled -> {
                updatePersistedQueueItem(failure.item)
                Log.w(
                    TAG,
                    "Retrying queued event=${item.event} in ${failure.delayMs}ms (attempt=${failure.item.attempts}) due to $reason"
                )
            }
            is SocketOutboundRetryQueue.FailureResult.Dropped -> {
                deletePersistedQueueItem(failure.item.queueId)
                Log.e(
                    TAG,
                    "Dropped queued event=${item.event} after ${failure.item.attempts} attempts due to $reason"
                )
                crashReporter.reportSocketOutboundQueueIssue(
                    issueType = "SocketOutboundQueueDropped",
                    event = item.event,
                    reason = reason,
                    attempts = failure.item.attempts,
                    dedupeKey = item.dedupeKey,
                )
            }
            null -> Unit
        }
    }

    private enum class PersistQueueResult {
        PERSISTED,
        DUPLICATE,
        FAILED,
    }

    private fun restorePersistedOutboundQueue() {
        val currentOwner = currentQueueOwner()
        val restoredItems = runCatching {
            runBlocking(Dispatchers.IO) {
                socketOutboundQueueStore.getAll()
            }
        }.getOrElse { error ->
            Log.e(TAG, "Failed to restore outbound queue from DB", error)
            crashReporter.reportSocketOutboundQueueIssue(
                issueType = "SocketOutboundQueueRestoreFailed",
                event = "restore",
                reason = error.message ?: "unknown",
                attempts = 0,
                dedupeKey = null,
            )
            emptyList()
        }

        if (restoredItems.isEmpty()) {
            return
        }

        val queueItems = restoredItems.mapNotNull { entity ->
            if (currentOwner.isNotBlank() && entity.ownerUsername != currentOwner) {
                deletePersistedQueueItem(entity.queueId)
                return@mapNotNull null
            }

            val payload = runCatching {
                SocketOutboundPayloadCodec.decode(
                    payloadType = entity.payloadType,
                    payloadBody = entity.payloadBody,
                )
            }.getOrElse { decodeError ->
                Log.e(TAG, "Dropping invalid persisted outbound payload queueId=${entity.queueId}", decodeError)
                deletePersistedQueueItem(entity.queueId)
                return@mapNotNull null
            }

            SocketOutboundRetryQueue.QueueItem(
                queueId = entity.queueId,
                event = entity.event,
                payload = payload,
                dedupeKey = entity.dedupeKey,
                attempts = entity.attempts,
                nextAttemptAtMs = entity.nextAttemptAtMs,
            )
        }

        synchronized(lock) {
            outboundRetryQueue.replaceAll(queueItems)
            syncOutboundQueueCountLocked()
        }

        crashReporter.addBreadcrumb(
            category = "socket",
            message = "Restored ${queueItems.size} queued outbound socket events from DB"
        )
    }

    private fun persistQueueItem(item: SocketOutboundRetryQueue.QueueItem): PersistQueueResult {
        val encodedPayload = runCatching {
            SocketOutboundPayloadCodec.encode(item.payload)
        }.getOrElse { error ->
            Log.e(TAG, "Failed to encode outbound payload queueId=${item.queueId}", error)
            return PersistQueueResult.FAILED
        }

        return runCatching {
            runBlocking(Dispatchers.IO) {
                socketOutboundQueueStore.insert(
                    SocketOutboundQueueRecord(
                        queueId = item.queueId,
                        event = item.event,
                        ownerUsername = currentQueueOwner(),
                        payloadType = encodedPayload.payloadType,
                        payloadBody = encodedPayload.payloadBody,
                        dedupeKey = item.dedupeKey,
                        attempts = item.attempts,
                        nextAttemptAtMs = item.nextAttemptAtMs,
                        createdAtMs = System.currentTimeMillis(),
                    )
                )
            }
        }.fold(
            onSuccess = { inserted ->
                if (inserted) PersistQueueResult.PERSISTED else PersistQueueResult.DUPLICATE
            },
            onFailure = { error ->
                Log.e(TAG, "Failed to persist outbound queue item queueId=${item.queueId}", error)
                PersistQueueResult.FAILED
            }
        )
    }

    private fun updatePersistedQueueItem(item: SocketOutboundRetryQueue.QueueItem) {
        runCatching {
            runBlocking(Dispatchers.IO) {
                socketOutboundQueueStore.updateRetryState(
                    queueId = item.queueId,
                    attempts = item.attempts,
                    nextAttemptAtMs = item.nextAttemptAtMs,
                )
            }
        }.onFailure { error ->
            Log.e(TAG, "Failed to update retry state for queueId=${item.queueId}", error)
        }
    }

    private fun deletePersistedQueueItem(queueId: String) {
        runCatching {
            runBlocking(Dispatchers.IO) {
                socketOutboundQueueStore.deleteByQueueId(queueId)
            }
        }.onFailure { error ->
            Log.e(TAG, "Failed to delete outbound queue item queueId=$queueId", error)
        }
    }

    private fun clearPersistedOutboundQueue() {
        runCatching {
            runBlocking(Dispatchers.IO) {
                socketOutboundQueueStore.clearAll()
            }
        }.onFailure { error ->
            Log.e(TAG, "Failed to clear persisted outbound queue", error)
        }
    }

    private fun currentQueueOwner(): String {
        return sprefManager.getUsername().orEmpty().trim()
    }

    private fun syncOutboundQueueCountLocked() {
        _queuedOutboundCount.value = outboundRetryQueue.size
    }
}
