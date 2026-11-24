package com.dino.message.corefeature.data.socket

import com.dino.message.corefeature.data.socket.SocketIoPacketCodec.IncomingPacket
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.websocket.WebSockets
import java.io.File
import java.util.UUID
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SocketIoBackendCompatibilityTest {
    @Test
    fun authenticatesAndExchangesEventsOverBothTransports() {
        val baseUrl = System.getenv("DINO_REALTIME_TEST_URL") ?: return
        val tokens = File(checkNotNull(System.getenv("DINO_REALTIME_TEST_TOKENS"))).readLines()
        HttpClient(CIO) { install(WebSockets) }.use { client ->
            runBlocking {
                for (forcePolling in listOf(false, true)) {
                    withTimeout(15_000) {
                        val connection = KtorSocketIoConnection(client, baseUrl, pollingOnly = forcePolling, allowPollingFallback = false)
                        val rejected = CompletableDeferred<Unit>()
                        connection.run("invalid") {
                            if (it is IncomingPacket.ConnectError) rejected.complete(Unit)
                        }
                        assertTrue(rejected.isCompleted)
                        for (token in tokens) {
                            val connected = CompletableDeferred<Unit>()
                            val event = CompletableDeferred<IncomingPacket.Event>()
                            val messageResult = CompletableDeferred<IncomingPacket.Event>()
                            val job = launch {
                                connection.run(token) {
                                    if (it == IncomingPacket.Connected) connected.complete(Unit)
                                    if (it is IncomingPacket.Event) when (it.name) {
                                        "user_status" -> event.complete(it)
                                        "message_sent", "message_error" -> messageResult.complete(it)
                                    }
                                }
                            }
                            try {
                                connected.await()
                                assertTrue(connection.send("get_user_status",
                                    JsonObject(mapOf("username" to JsonPrimitive("alice")))))
                                assertEquals(
                                    JsonObject(mapOf("username" to JsonPrimitive("alice"), "online" to JsonPrimitive(true))),
                                    event.await().arguments.single(),
                                )
                                val messageId = UUID.randomUUID().toString()
                                assertTrue(connection.send("send_message", JsonObject(mapOf(
                                    "to" to JsonPrimitive("bob"),
                                    "message" to JsonPrimitive("test-ciphertext"),
                                    "encrypted_key" to JsonPrimitive("test-key"),
                                    "client_message_id" to JsonPrimitive(messageId),
                                ))))
                                val response = messageResult.await()
                                assertEquals("message_sent", response.name, response.arguments.toString())
                                val body = response.arguments.single() as JsonObject
                                assertEquals(JsonPrimitive(messageId), body["client_message_id"])
                                assertTrue("message_id" in body)
                            } finally {
                                job.cancelAndJoin()
                            }
                        }
                    }
                }
            }
        }
    }
}
