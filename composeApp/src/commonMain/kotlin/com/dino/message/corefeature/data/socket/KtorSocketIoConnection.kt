package com.dino.message.corefeature.data.socket

import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.URLBuilder
import io.ktor.http.URLProtocol
import io.ktor.http.contentType
import io.ktor.http.encodedPath
import io.ktor.http.isSuccess
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long

internal interface SocketIoConnection {
    suspend fun run(token: String, onPacket: suspend (SocketIoPacketCodec.IncomingPacket) -> Unit)
    suspend fun send(event: String, payload: Any?): Boolean
}

internal class KtorSocketIoConnection(
    private val client: HttpClient,
    baseUrl: String,
    private val pollingOnly: Boolean = false,
    private val allowPollingFallback: Boolean = true,
) : SocketIoConnection {
    private val endpoint = URLBuilder(baseUrl).apply {
        require(protocol == URLProtocol.HTTP || protocol == URLProtocol.HTTPS)
        encodedPath = "/socket.io/"
        parameters.clear()
        fragment = ""
    }.buildString()
    private val postLock = Mutex()
    private val sender = MutableStateFlow<(suspend (String) -> Unit)?>(null)

    override suspend fun send(event: String, payload: Any?): Boolean {
        val send = sender.value ?: return false
        return try {
            send(SocketIoPacketCodec.eventPacket(event, payload))
            true
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            false
        }
    }

    override suspend fun run(
        token: String,
        onPacket: suspend (SocketIoPacketCodec.IncomingPacket) -> Unit,
    ) {
        var opened = false
        if (!pollingOnly) try {
            val websocketUrl = URLBuilder(endpoint).apply {
                protocol = if (protocol == URLProtocol.HTTPS) URLProtocol.WSS else URLProtocol.WS
                parameters.append("EIO", "4")
                parameters.append("transport", "websocket")
            }.buildString()
            client.webSocket(websocketUrl) {
                exchange(token, { listOf((incoming.receive() as Frame.Text).readText()) },
                    { outgoing.send(Frame.Text(it)) }, { opened = true }, onPacket)
            }
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            if (opened || !allowPollingFallback) throw error
        }
        if (opened) return

        var sid: String? = null
        val postPacket: suspend (String) -> Unit = { packet ->
            postLock.withLock {
                val response = client.post(endpoint) {
                    parameter("EIO", "4")
                    parameter("transport", "polling")
                    parameter("sid", checkNotNull(sid))
                    contentType(ContentType.Text.Plain)
                    setBody(packet)
                }
                check(response.status.isSuccess() && response.bodyAsText() == "OK") {
                    "Engine.IO polling POST failed"
                }
            }
        }
        try {
            exchange(token, {
                val response = client.get(endpoint) {
                    parameter("EIO", "4")
                    parameter("transport", "polling")
                    sid?.let { parameter("sid", it) }
                }
                check(response.status.isSuccess()) { "Engine.IO polling GET failed" }
                response.bodyAsText().split('\u001e')
            }, postPacket, { sid = it }, onPacket)
        } finally {
            if (sid != null) withContext(NonCancellable) {
                withTimeoutOrNull(1_000) { runCatching { postPacket("1") } }
            }
        }
    }

    private suspend fun exchange(
        token: String,
        receive: suspend () -> List<String>,
        transmit: suspend (String) -> Unit,
        onOpen: (String) -> Unit,
        onPacket: suspend (SocketIoPacketCodec.IncomingPacket) -> Unit,
    ) {
        var timeoutMs = 20_000L
        var connected = false
        var opened = false
        val pending = mutableListOf<SocketIoPacketCodec.IncomingPacket.Event>()
        try {
            while (true) {
                val frames = withTimeoutOrNull(timeoutMs) { receive() }
                    ?: error("Engine.IO receive timed out")
                for (frame in frames) {
                    when (val packet = SocketIoPacketCodec.decode(frame)) {
                        SocketIoPacketCodec.IncomingPacket.EngineOpen -> {
                            check(!opened)
                            val handshake = Json.parseToJsonElement(frame.drop(1)).jsonObject
                            val interval = handshake.getValue("pingInterval").jsonPrimitive.long
                            val timeout = handshake.getValue("pingTimeout").jsonPrimitive.long
                            require(interval in 1..300_000 && timeout in 1..300_000)
                            timeoutMs = interval + timeout
                            onOpen(handshake.getValue("sid").jsonPrimitive.content)
                            opened = true
                            transmit(SocketIoPacketCodec.connectPacket(token))
                        }
                        is SocketIoPacketCodec.IncomingPacket.Ping ->
                            transmit(SocketIoPacketCodec.pongPacket(packet.payload))
                        SocketIoPacketCodec.IncomingPacket.Connected -> {
                            check(opened && !connected)
                            connected = true
                            sender.value = transmit
                            onPacket(packet)
                            pending.forEach { onPacket(it) }
                            pending.clear()
                        }
                        is SocketIoPacketCodec.IncomingPacket.ConnectError,
                        SocketIoPacketCodec.IncomingPacket.Disconnected -> {
                            onPacket(packet)
                            return
                        }
                        SocketIoPacketCodec.IncomingPacket.EngineClose -> return
                        is SocketIoPacketCodec.IncomingPacket.Event -> {
                            if (connected) onPacket(packet)
                            else if (opened) {
                                check(pending.size < 500) { "Too many pre-connect events" }
                                pending.add(packet)
                            }
                        }
                        SocketIoPacketCodec.IncomingPacket.Ignore -> Unit
                    }
                }
            }
        } finally {
            sender.value = null
        }
    }
}
