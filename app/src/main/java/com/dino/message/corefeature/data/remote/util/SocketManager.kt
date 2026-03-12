package com.dino.message.corefeature.data.remote.util

import android.util.Log
import com.dino.message.BuildConfig
import com.dino.message.chatfeature.data.remote.dto.MessageAttachment
import com.dino.message.chatfeature.data.remote.dto.MessageSent
import com.dino.message.chatfeature.data.remote.dto.NewMessage
import com.dino.message.chatfeature.data.remote.dto.PendingMessage
import com.dino.message.corefeature.data.spref.SPrefManager
import io.socket.client.IO
import io.socket.client.Socket
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.net.URI
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SocketManager @Inject constructor(
    private val sPrefManager: SPrefManager
) {

    private var socket: Socket? = null

    suspend fun connectAndListen(
        onConnected: (String) -> Unit,
        onPendingMessages: (List<PendingMessage>) -> Unit,
        onNewMessage: (NewMessage) -> Unit,
        onMessageSent: (MessageSent) -> Unit,
        onMessageError: (String) -> Unit,
        onUserStatus: (username: String, online: Boolean) -> Unit,
        callRefreshTokenAPI: suspend () -> String
    ) {
        if (socket?.connected() == true) return

        val token = sPrefManager.getToken().orEmpty()
        if (token.isBlank()) {
            onMessageError("Missing access token")
            return
        }

        val baseUri = URI(BuildConfig.BASE_URL)
        val serverUrl = "${baseUri.scheme}://${baseUri.host}" +
            (if (baseUri.port != -1) ":${baseUri.port}" else "")

        socket?.off()
        socket?.disconnect()

        val options = IO.Options.builder()
            .setPath("/socket.io")
            .setAuth(mapOf("token" to token))
            .setTransports(arrayOf(WebSocketTransport))
            .build()

        socket = IO.socket(serverUrl, options)

        socket?.on(Socket.EVENT_CONNECT) {
            Log.d(TAG, "Connected to socket")
        }

        socket?.on("connected") { args ->
            val payload = args.firstOrNull().asJsonObjectOrNull()
            val username = payload?.optString("username").orEmpty()
            onConnected(username)
        }

        socket?.on("pending_messages") { args ->
            val payload = args.firstOrNull().asJsonObjectOrNull() ?: JSONObject()
            val messagesJson = payload.optJSONArray("messages") ?: JSONArray()
            onPendingMessages(parsePendingMessages(messagesJson))
        }

        socket?.on("new_message") { args ->
            val payload = args.firstOrNull().asJsonObjectOrNull() ?: return@on
            parseNewMessage(payload)?.let(onNewMessage)
        }

        socket?.on("message_sent") { args ->
            val payload = args.firstOrNull().asJsonObjectOrNull() ?: return@on
            onMessageSent(
                MessageSent(
                    to = payload.optString("to"),
                    type = payload.optString("type", MessageTypeText),
                    timestamp = payload.optString("timestamp")
                )
            )
        }

        socket?.on("user_status") { args ->
            val payload = args.firstOrNull().asJsonObjectOrNull() ?: return@on
            onUserStatus(payload.optString("username"), payload.optBoolean("online"))
        }

        socket?.on("message_error") { args ->
            val payload = args.firstOrNull().asJsonObjectOrNull()
            onMessageError(payload?.optString("error").orEmpty())
        }

        socket?.on(Socket.EVENT_CONNECT_ERROR) { args ->
            onMessageError(args.firstOrNull()?.toString() ?: "Socket connection error")
        }

        socket?.on(Socket.EVENT_DISCONNECT) {
            Log.d(TAG, "Disconnected from socket")
            CoroutineScope(Dispatchers.IO).launch {
                reconnectWithToken(
                    onConnected = onConnected,
                    onPendingMessages = onPendingMessages,
                    onNewMessage = onNewMessage,
                    onMessageSent = onMessageSent,
                    onMessageError = onMessageError,
                    onUserStatus = onUserStatus,
                    callRefreshTokenAPI = callRefreshTokenAPI
                )
            }
        }

        socket?.connect()
    }

    fun sendMessage(
        to: String,
        message: String,
        encryptedKey: String,
        type: String = MessageTypeText,
        attachment: MessageAttachment? = null
    ) {
        val payload = JSONObject().apply {
            put("to", to)
            put("type", type)
            put("message", message)
            put("encrypted_key", encryptedKey)

            attachment?.let {
                put(
                    "attachment",
                    JSONObject().apply {
                        put("type", it.type)
                        put("url", it.url)
                        put("mime_type", it.mimeType)
                        it.fileName?.let { fileName -> put("file_name", fileName) }
                        it.sizeBytes?.let { sizeBytes -> put("size_bytes", sizeBytes) }
                    }
                )
            }
        }
        socket?.emit("send_message", payload)
    }

    fun getUserStatus(username: String) {
        socket?.emit("get_user_status", JSONObject().put("username", username))
    }

    private suspend fun reconnectWithToken(
        onConnected: (String) -> Unit,
        onPendingMessages: (List<PendingMessage>) -> Unit,
        onNewMessage: (NewMessage) -> Unit,
        onMessageSent: (MessageSent) -> Unit,
        onMessageError: (String) -> Unit,
        onUserStatus: (username: String, online: Boolean) -> Unit,
        callRefreshTokenAPI: suspend () -> String
    ) {
        val newToken = callRefreshTokenAPI()
        if (newToken.isNotBlank()) {
            sPrefManager.setToken(newToken)
            connectAndListen(
                onConnected = onConnected,
                onPendingMessages = onPendingMessages,
                onNewMessage = onNewMessage,
                onMessageSent = onMessageSent,
                onMessageError = onMessageError,
                onUserStatus = onUserStatus,
                callRefreshTokenAPI = callRefreshTokenAPI
            )
        }
    }

    fun disconnect() {
        socket?.off()
        socket?.disconnect()
        socket = null
    }

    private fun parsePendingMessages(messagesJson: JSONArray): List<PendingMessage> {
        val messages = mutableListOf<PendingMessage>()
        for (i in 0 until messagesJson.length()) {
            val item = messagesJson.optJSONObject(i) ?: continue
            messages.add(
                PendingMessage(
                    from = item.optString("from"),
                    type = item.optString("type", MessageTypeText),
                    message = item.optString("message"),
                    encryptedKey = item.optString("encrypted_key"),
                    attachment = item.optJSONObject("attachment")?.toAttachment(),
                    timestamp = item.optString("timestamp")
                )
            )
        }
        return messages
    }

    private fun parseNewMessage(payload: JSONObject): NewMessage? {
        val sender = payload.optString("from")
        if (sender.isBlank()) return null

        return NewMessage(
            from = sender,
            type = payload.optString("type", MessageTypeText),
            message = payload.optString("message"),
            encryptedKey = payload.optString("encrypted_key"),
            attachment = payload.optJSONObject("attachment")?.toAttachment(),
            timestamp = payload.optString("timestamp")
        )
    }

    private fun JSONObject.toAttachment(): MessageAttachment {
        return MessageAttachment(
            type = optString("type"),
            url = optString("url"),
            mimeType = optString("mime_type"),
            fileName = optString("file_name").takeIf { it.isNotBlank() },
            sizeBytes = optLong("size_bytes").takeIf { it > 0 }
        )
    }

    private fun Any?.asJsonObjectOrNull(): JSONObject? {
        return when (this) {
            is JSONObject -> this
            is String -> runCatching { JSONObject(this) }.getOrNull()
            else -> null
        }
    }

    companion object {
        private const val TAG = "SocketManager"
        private const val WebSocketTransport = "websocket"
        private const val MessageTypeText = "text"
    }
}
