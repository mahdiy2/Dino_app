package com.dino.message.corefeature.data.socket

import com.dino.message.corefeature.data.remote.logging.AppLogger
import io.socket.client.IO
import io.socket.client.Socket
import io.socket.emitter.Emitter
import java.net.URISyntaxException

class AndroidSocketIoTransportFactory(
    private val baseUrl: String,
    private val appLogger: AppLogger,
) : RealtimeSocketTransportFactory {

    override fun create(token: String): RealtimeSocketTransport? {
        val normalizedBaseUrl = baseUrl.trimEnd('/')
        val options = IO.Options.builder()
            .setForceNew(false)
            .setMultiplex(true)
            .setReconnection(true)
            .setReconnectionAttempts(Int.MAX_VALUE)
            .setReconnectionDelay(1_000)
            .setReconnectionDelayMax(10_000)
            .setTimeout(20_000)
            .setPath("/socket.io/")
            .setTransports(arrayOf("websocket", "polling"))
            .setAuth(mapOf("token" to token))
            .build()

        return try {
            AndroidSocketIoTransport(IO.socket(normalizedBaseUrl, options))
        } catch (e: URISyntaxException) {
            appLogger.error(TAG, "Invalid socket URL: $normalizedBaseUrl", e)
            null
        } catch (e: Exception) {
            appLogger.error(TAG, "Failed to create socket", e)
            null
        }
    }

    private companion object {
        private const val TAG = "AndroidSocketIoTransport"
    }
}

private class AndroidSocketIoTransport(
    private val socket: Socket,
) : RealtimeSocketTransport {
    private val listenerAdapters = mutableMapOf<RealtimeSocketListener, Emitter.Listener>()

    override fun connect() {
        socket.connect()
    }

    override fun disconnect() {
        socket.disconnect()
    }

    override fun offAll() {
        socket.off()
        listenerAdapters.clear()
    }

    override fun isConnected(): Boolean {
        return socket.connected()
    }

    override fun on(event: String, listener: RealtimeSocketListener) {
        val adapter = listenerAdapters.getOrPut(listener) {
            Emitter.Listener { args -> listener.onEvent(args) }
        }
        socket.on(event, adapter)
    }

    override fun off(event: String, listener: RealtimeSocketListener) {
        val adapter = listenerAdapters.remove(listener) ?: return
        socket.off(event, adapter)
    }

    override fun emit(event: String, payload: Any?) {
        if (payload == null) {
            socket.emit(event)
        } else {
            socket.emit(event, payload)
        }
    }
}
