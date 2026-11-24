package com.dino.message.corefeature.data.socket

import com.dino.message.corefeature.domain.session.AuthSessionStore
import com.dino.message.corefeature.domain.socket.RealtimeSocketEvents
import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.WebSockets
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonObject
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/** Owns connection and queued sends for one app lifecycle. Payload objects and arrays use kotlinx JSON. */
class SharedRealtimeGateway internal constructor(
    private val username: () -> String?,
    private val accessToken: () -> String,
    private val store: SocketOutboundQueueStore,
    private val scope: CoroutineScope,
    private val now: () -> Long,
    private val createConnection: () -> SocketIoConnection,
) {
    enum class State { DISCONNECTED, CONNECTING, CONNECTED, AUTH_REJECTED }
    enum class SendResult { SENT, QUEUED, ALREADY_QUEUED, QUEUE_FULL, DISCONNECTED, STORAGE_FAILED }

    private val lock = Mutex()
    private val wake = Channel<Unit>(Channel.CONFLATED)
    private val listeners = mutableMapOf<Pair<String, String>, (List<Any?>) -> Unit>()
    private val state = MutableStateFlow(State.DISCONNECTED)
    private val count = MutableStateFlow(0)
    val connectionState = state.asStateFlow()
    val queuedOutboundCount = count.asStateFlow()

    @OptIn(ExperimentalUuidApi::class)
    private val queue = SocketOutboundRetryQueue(5, 1_000, 15_000, 200, now) { Uuid.random().toString() }
    private var owner: String? = null
    private var enabled = false
    private val awaitingDeletion = mutableSetOf<String>()
    private var token: String? = null
    private var connection: SocketIoConnection? = null
    private var connectionJob: Job? = null

    init {
        scope.launch {
            for (signal in wake) {
                val wait = lock.withLock { flushLocked() } ?: continue
                withTimeoutOrNull(wait) { wake.receive() }
                wake.trySend(Unit)
            }
        }
    }

    suspend fun registerListener(owner: String, event: String, listener: (List<Any?>) -> Unit) {
        lock.withLock { listeners[owner to event] = listener }
    }

    suspend fun unregisterOwner(owner: String) {
        lock.withLock { listeners.keys.removeAll { it.first == owner } }
    }

    suspend fun connect(forceReconnect: Boolean = false): Boolean =
        lock.withLock {
            enabled = true
            connectLocked(forceReconnect)
        }

    suspend fun disconnect(clearListeners: Boolean = false) {
        lock.withLock {
            enabled = false
            retireLocked()
            if (clearListeners) {
                listeners.clear()
                val account = owner ?: username()
                store.getAll().filter { it.ownerUsername == account }.forEach { store.deleteByQueueId(it.queueId) }
            }
            owner = null
            queue.clear()
            count.value = 0
        }
    }

    suspend fun send(event: String, payload: Any? = null, dedupeKey: String? = null): SendResult =
        lock.withLock {
            enabled = true
            if (!storageCall { connectLocked(false) }.success) return@withLock SendResult.STORAGE_FAILED
            val account = owner ?: return@withLock SendResult.DISCONNECTED
            if (token == null) return@withLock SendResult.DISCONNECTED
            if (sendConnected(event, payload)) return@withLock SendResult.SENT
            val key = dedupeKey ?: SocketRetryPolicy.autoDedupeKey(event, payload as? JsonObject)
            if (key == null && !SocketRetryPolicy.requiresRetry(event)) return@withLock SendResult.DISCONNECTED
            when (val result = queue.enqueue(event, payload, key)) {
                SocketOutboundRetryQueue.EnqueueResult.AlreadyQueued -> SendResult.ALREADY_QUEUED
                SocketOutboundRetryQueue.EnqueueResult.QueueFull -> SendResult.QUEUE_FULL
                is SocketOutboundRetryQueue.EnqueueResult.Queued -> {
                    val item = result.item
                    val inserted = withContext(NonCancellable) {
                        storageCall {
                            val encoded = SocketOutboundPayloadCodec.encodeValue(payload)
                            store.insert(SocketOutboundQueueRecord(
                                item.queueId, event, account, encoded.payloadType, encoded.payloadBody,
                                item.dedupeKey?.let { dedupePrefix(account) + it },
                                item.attempts, item.nextAttemptAtMs, now(),
                            ))
                        }
                    }
                    if (inserted.value != true) queue.markSuccess(item.queueId)
                    count.value = queue.size
                    wake.trySend(Unit)
                    when {
                        !inserted.success -> SendResult.STORAGE_FAILED
                        inserted.value == true -> SendResult.QUEUED
                        else -> SendResult.ALREADY_QUEUED
                    }
                }
            }
        }

    private suspend fun connectLocked(force: Boolean): Boolean {
        val account = username()?.trim()?.takeIf { it.isNotEmpty() }
        val credential = accessToken().trim().let {
            if (it.startsWith("Bearer ", ignoreCase = true)) it.drop(7).trim() else it
        }.takeIf { it.isNotEmpty() }
        val accountChanged = account != owner
        if (accountChanged || credential != token || force) retireLocked()
        if (accountChanged) {
            val items = if (account == null) emptyList() else store.getAll()
                .filter { it.ownerUsername == account }
                .mapNotNull { row ->
                    val payload = storageCall { SocketOutboundPayloadCodec.decodeValue(row.payloadType, row.payloadBody) }
                    if (!payload.success) {
                        store.deleteByQueueId(row.queueId)
                        null
                    } else SocketOutboundRetryQueue.QueueItem(
                        row.queueId, row.event, payload.value,
                        row.dedupeKey?.removePrefix(dedupePrefix(account)), row.attempts, row.nextAttemptAtMs,
                    )
                }
            queue.replaceAll(items)
            owner = account
            count.value = queue.size
        }
        if (account == null || credential == null) return false
        if (connectionJob?.isActive == true || state.value == State.AUTH_REJECTED) return true
        val target = createConnection()
        token = credential
        connection = target
        state.value = State.CONNECTING
        connectionJob = scope.launch { maintainConnection(target, credential) }
        wake.trySend(Unit)
        return true
    }

    private fun retireLocked() {
        connection = null
        token = null
        connectionJob?.cancel()
        connectionJob = null
        state.value = State.DISCONNECTED
    }

    private suspend fun maintainConnection(target: SocketIoConnection, credential: String) {
        var backoff = 1_000L
        while (true) {
            var connected = false
            var rejected = false
            var disconnected = false
            try {
                target.run(credential) { packet ->
                    when (packet) {
                        SocketIoPacketCodec.IncomingPacket.Connected -> {
                            connected = true
                            notify(target, RealtimeSocketEvents.CONNECT, emptyList(), State.CONNECTED)
                        }
                        is SocketIoPacketCodec.IncomingPacket.ConnectError -> {
                            rejected = true
                            notify(target, RealtimeSocketEvents.CONNECT_ERROR, listOf(packet.payload), State.AUTH_REJECTED)
                        }
                        SocketIoPacketCodec.IncomingPacket.Disconnected -> {
                            disconnected = true
                            notify(target, RealtimeSocketEvents.DISCONNECT, listOf("io server disconnect"), State.DISCONNECTED)
                        }
                        is SocketIoPacketCodec.IncomingPacket.Event ->
                            notify(target, packet.name, packet.arguments)
                        else -> Unit
                    }
                }
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                // A failed network attempt goes through the same reconnect path as a closed stream.
            }
            if (rejected || disconnected) return
            notify(target,
                if (connected) RealtimeSocketEvents.DISCONNECT else RealtimeSocketEvents.CONNECT_ERROR,
                listOf(if (connected) "transport close" else "connection failed"), State.CONNECTING)
            if (connected) backoff = 1_000
            delay(backoff)
            backoff = (backoff * 2).coerceAtMost(10_000)
            if (lock.withLock { connection !== target || !connectLocked(false) || connection !== target }) return
        }
    }

    private suspend fun notify(target: SocketIoConnection, event: String, args: List<Any?>, next: State? = null) {
        val callbacks = lock.withLock {
            if (connection !== target) return
            next?.let { state.value = it }
            wake.trySend(Unit)
            listeners.filterKeys { it.second == event }.values.toList()
        }
        callbacks.forEach { callback -> runCatching { callback(args) } }
    }

    private suspend fun sendConnected(event: String, payload: Any?): Boolean =
        state.value == State.CONNECTED &&
            withTimeoutOrNull(7_000) { connection?.send(event, payload) } == true

    private suspend fun flushLocked(): Long? {
        if (!enabled) return null
        if (!storageCall { connectLocked(false) }.success) return 1_000
        val wait = queue.millisUntilNextReady() ?: return null
        if (wait > 0) return wait
        if (state.value != State.CONNECTED) return null
        val item = queue.peekReady() ?: return 1
        if (item.queueId in awaitingDeletion || sendConnected(item.event, item.payload)) {
            awaitingDeletion.add(item.queueId)
            if (!storageCall { store.deleteByQueueId(item.queueId) }.success) return 1_000
            awaitingDeletion.remove(item.queueId)
            queue.markSuccess(item.queueId)
        } else {
            val persisted = when (val failure = queue.markFailure(item.queueId)) {
                is SocketOutboundRetryQueue.FailureResult.RetryScheduled -> storageCall {
                    store.updateRetryState(item.queueId, failure.item.attempts, failure.item.nextAttemptAtMs)
                }.success
                is SocketOutboundRetryQueue.FailureResult.Dropped ->
                    storageCall { store.deleteByQueueId(item.queueId) }.success
                null -> true
            }
            if (!persisted) {
                queue.restoreItem(item)
                return 1_000
            }
        }
        count.value = queue.size
        return 1
    }

    private fun dedupePrefix(account: String): String = "${account.length}:$account:"

    private data class StorageResult<T>(val success: Boolean, val value: T?)
    private suspend fun <T> storageCall(block: suspend () -> T): StorageResult<T> = try {
        StorageResult(true, block())
    } catch (error: CancellationException) {
        throw error
    } catch (_: Exception) {
        StorageResult(false, null)
    }
}

fun createSharedRealtimeGateway(
    baseUrl: String,
    session: AuthSessionStore,
    store: SocketOutboundQueueStore,
    scope: CoroutineScope,
    currentTimeMillis: () -> Long,
): SharedRealtimeGateway {
    val lifecycle = checkNotNull(scope.coroutineContext[Job]) { "Realtime requires a lifecycle scope" }
    val client = HttpClient { install(WebSockets) }
    lifecycle.invokeOnCompletion { client.close() }
    return SharedRealtimeGateway(session::getUsername, session::getToken, store, scope, currentTimeMillis) {
        KtorSocketIoConnection(client, baseUrl)
    }
}
