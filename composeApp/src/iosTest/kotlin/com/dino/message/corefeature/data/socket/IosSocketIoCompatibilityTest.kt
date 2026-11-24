package com.dino.message.corefeature.data.socket

import com.dino.message.corefeature.data.socket.SocketIoPacketCodec.IncomingPacket
import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.WebSockets
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.toKString
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import platform.posix.getenv
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalForeignApi::class)
class IosSocketIoCompatibilityTest {
    @Test
    fun exchangesAuthenticatedEventsUsingBothTransports() {
        val baseUrl = getenv("DINO_REALTIME_TEST_URL")?.toKString() ?: return
        val token = checkNotNull(getenv("DINO_REALTIME_TEST_TOKEN")?.toKString())
        HttpClient { install(WebSockets) }.use { client ->
            runBlocking {
                for (polling in listOf(false, true)) {
                    withTimeout(15_000) {
                        val connection = KtorSocketIoConnection(
                            client, baseUrl, pollingOnly = polling, allowPollingFallback = false,
                        )
                        val connected = CompletableDeferred<Unit>()
                        val status = CompletableDeferred<List<Any?>>()
                        val job = launch {
                            connection.run(token) { packet ->
                                if (packet == IncomingPacket.Connected) connected.complete(Unit)
                                if (packet is IncomingPacket.Event && packet.name == "user_status") {
                                    status.complete(packet.arguments)
                                }
                            }
                        }
                        try {
                            connected.await()
                            val username = JsonPrimitive("alice")
                            assertTrue(connection.send("get_user_status", JsonObject(mapOf("username" to username))))
                            assertEquals(
                                JsonObject(mapOf("username" to username, "online" to JsonPrimitive(true))),
                                status.await().single(),
                            )
                        } finally {
                            job.cancelAndJoin()
                        }
                    }
                }
            }
        }
    }
}
