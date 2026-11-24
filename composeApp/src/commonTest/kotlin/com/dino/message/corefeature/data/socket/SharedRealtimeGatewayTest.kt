package com.dino.message.corefeature.data.socket

import com.dino.message.corefeature.data.socket.SocketIoPacketCodec.IncomingPacket
import com.dino.message.corefeature.data.socket.SharedRealtimeGateway.SendResult
import com.dino.message.corefeature.domain.lifecycle.AppLifecycleDispatcher
import com.dino.message.corefeature.domain.lifecycle.AppLifecycleEvent
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class SharedRealtimeGatewayTest {
    @Test
    fun mapsSocketIoEventsAndRoundTripsDurablePayloads() {
        val payload = Json.parseToJsonElement("""{"list":[1,null,true],"text":"quoted"}""")
        assertEquals("""40{"token":"a\"b"}""", SocketIoPacketCodec.connectPacket("a\"b"))
        assertEquals("3probe", SocketIoPacketCodec.pongPacket("probe"))
        assertEquals(IncomingPacket.Event("pending_messages", listOf(payload, null)),
            SocketIoPacketCodec.decode("""42["pending_messages",$payload,null]"""))
        assertEquals(IncomingPacket.Ignore, SocketIoPacketCodec.decode("42[1]"))
        for (value in listOf(null, payload, true, 123L, "text")) {
            val encoded = SocketOutboundPayloadCodec.encodeValue(value)
            assertEquals(value, SocketOutboundPayloadCodec.decodeValue(encoded.payloadType, encoded.payloadBody))
        }
    }

    @Test
    fun restoresQueuedMessagesAndWaitsForFreshAuth() = runTest {
        val store = MemoryStore()
        var token = "old"
        val connections = mutableListOf<FakeConnection>()
        fun gateway() = SharedRealtimeGateway({ "alice" }, { token }, store, backgroundScope,
            { testScheduler.currentTime }) { FakeConnection().also(connections::add) }
        val payload = Json.parseToJsonElement("""{"to":"bob","client_message_id":"id"}""")
        val original = gateway()
        assertEquals(SendResult.QUEUED, original.send("send_message", payload))
        assertEquals(SendResult.ALREADY_QUEUED, original.send("send_message", payload))
        original.disconnect()
        assertEquals(1, store.rows.size)

        val restored = gateway()
        val received = mutableListOf<List<Any?>>()
        restored.registerListener("chat", "new_message", received::add)
        assertTrue(restored.connect())
        runCurrent()
        val rejected = connections.last()
        rejected.packets.send(IncomingPacket.ConnectError("expired"))
        runCurrent()
        advanceTimeBy(20_000)
        assertEquals(SharedRealtimeGateway.State.AUTH_REJECTED, restored.connectionState.value)
        token = "new"
        restored.connect()
        runCurrent()
        val active = connections.last()
        assertEquals("new", active.tokens.single())
        active.packets.send(IncomingPacket.Connected)
        active.packets.send(IncomingPacket.Event("new_message", listOf(payload)))
        runCurrent()
        assertEquals(listOf<List<Any?>>(listOf(payload)), received)
        assertEquals(listOf<Pair<String, Any?>>("send_message" to payload), active.sent)
        assertTrue(store.rows.isEmpty())

        active.packets.send(IncomingPacket.EngineClose)
        runCurrent()
        advanceTimeBy(1_000)
        runCurrent()
        assertEquals(listOf("new", "new"), active.tokens)
    }

    @Test
    fun accountSwitchKeepsOtherAccountsQueueIsolated() = runTest {
        val store = MemoryStore()
        var owner = "alice"
        val gateway = SharedRealtimeGateway({ owner }, { "token" }, store, backgroundScope,
            { testScheduler.currentTime }) { FakeConnection() }
        assertEquals(SendResult.QUEUED, gateway.send("send_message", "alice payload", "same-id"))
        owner = "bob"
        assertEquals(SendResult.QUEUED, gateway.send("send_message", "bob payload", "same-id"))
        assertEquals(1, gateway.queuedOutboundCount.value)
        gateway.disconnect(clearListeners = true)
        assertEquals(listOf("alice"), store.getAll().map { it.ownerUsername })
    }

    @Test
    fun foregroundRestoresQueuedSendsAfterBackgroundSuspension() = runTest {
        val store = MemoryStore()
        val connections = mutableListOf<FakeConnection>()
        val gateway = SharedRealtimeGateway({ "alice" }, { "token" }, store, backgroundScope,
            { testScheduler.currentTime }) { FakeConnection().also(connections::add) }
        val dispatcher = AppLifecycleDispatcher()
        val binding = RealtimeRetryLifecycle(dispatcher, backgroundScope, gateway, suspendInBackground = true)
        try {
            dispatcher.dispatch(AppLifecycleEvent.ApplicationStarted)
            runCurrent()
            assertEquals(SendResult.QUEUED, gateway.send("send_message", "pending", "one"))
            dispatcher.dispatch(AppLifecycleEvent.EnteredBackground)
            runCurrent()
            assertEquals(SharedRealtimeGateway.State.DISCONNECTED, gateway.connectionState.value)
            binding.networkAvailable()
            runCurrent()
            assertEquals(1, connections.size)
            assertEquals(1, store.rows.size)

            dispatcher.dispatch(AppLifecycleEvent.ScreenStarted("chat", activeScreenCount = 1))
            runCurrent()
            assertEquals(2, connections.size)
            connections.last().packets.send(IncomingPacket.Connected)
            runCurrent()
            assertEquals(listOf<Pair<String, Any?>>("send_message" to "pending"), connections.last().sent)
            assertTrue(store.rows.isEmpty())
        } finally {
            binding.close()
        }
    }

    private class FakeConnection : SocketIoConnection {
        val packets = Channel<IncomingPacket>(Channel.UNLIMITED)
        val tokens = mutableListOf<String>()
        val sent = mutableListOf<Pair<String, Any?>>()

        override suspend fun run(token: String, onPacket: suspend (IncomingPacket) -> Unit) {
            tokens += token
            for (packet in packets) {
                if (packet == IncomingPacket.EngineClose) return
                onPacket(packet)
                if (packet is IncomingPacket.ConnectError) return
            }
        }

        override suspend fun send(event: String, payload: Any?): Boolean {
            sent += event to payload
            return true
        }
    }

    private class MemoryStore : SocketOutboundQueueStore {
        val rows = mutableMapOf<String, SocketOutboundQueueRecord>()
        override suspend fun getAll() = rows.values.toList()
        override suspend fun insert(item: SocketOutboundQueueRecord): Boolean {
            if (rows.containsKey(item.queueId) ||
                (item.dedupeKey != null && rows.values.any { it.dedupeKey == item.dedupeKey })
            ) return false
            rows[item.queueId] = item
            return true
        }
        override suspend fun deleteByQueueId(queueId: String) { rows.remove(queueId) }
        override suspend fun updateRetryState(queueId: String, attempts: Int, nextAttemptAtMs: Long) {
            rows[queueId]?.let { rows[queueId] = it.copy(attempts = attempts, nextAttemptAtMs = nextAttemptAtMs) }
        }
        override suspend fun clearAll() { rows.clear() }
    }
}
