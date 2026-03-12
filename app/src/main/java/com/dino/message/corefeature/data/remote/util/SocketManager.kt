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
    @Volatile
    private var isRefreshingSocketToken = false

    @Volatile
    private var isRefreshingSocketToken = false

    suspend fun connectAndListen(
        onConnected: (String) -> Unit,
        onPendingMessages: (List<PendingMessage>) -> Unit,
        onNewMessage: (NewMessage) -> Unit,
        onMessageSent: (MessageSent) -> Unit,
        onMessageError: (String) -> Unit,
        onUserStatus: (username: String, online: Boolean) -> Unit,
        callRefreshTokenAPI: suspend () -> String
    ) {
        if (socket?.connected() == true) {
            Log.d(TAG, "connectAndListen: socket already connected, rebinding listeners by recreating socket")
        }

        val token = sPrefManager.getToken().normalizeToken()
        if (token.isBlank()) {
            Log.w(TAG, "connectAndListen: access token is empty, socket connection aborted")
            onMessageError("Missing access token")
            return
        }

        val baseUri = URI(BuildConfig.BASE_URL)
        val serverUrl = "${baseUri.scheme}://${baseUri.host}" +
            (if (baseUri.port != -1) ":${baseUri.port}" else "")

        Log.d(TAG, "connectAndListen: preparing socket connection to $serverUrl")

        socket?.off()
        socket?.disconnect()

        val options = IO.Options.builder()
            .setPath("/socket.io")
            .setAuth(mapOf("token" to token))
            .setQuery("token=$token")
            .setExtraHeaders(mapOf("Authorization" to listOf("Bearer $token")))
            .setTransports(arrayOf(WebSocketTransport))
            .build()

        socket = IO.socket(serverUrl, options)

        socket?.on(Socket.EVENT_CONNECT) {
            Log.d(TAG, "Connected to socket successfully")
            val fallbackUsername = sPrefManager.getUsername().orEmpty()
            onConnected(fallbackUsername)
        }

        socket?.on("connected") { args ->
            val payload = args.firstOrNull().asJsonObjectOrNull()
            val username = payload?.optString("username")
                ?.takeIf { it.isNotBlank() }
                ?: sPrefManager.getUsername().orEmpty()
            Log.d(TAG, "connected event received. username=$username raw=${args.firstOrNull()}")
            onConnected(username)
        }

        socket?.on("pending_messages") { args ->
            val messagesJson = args.firstOrNull().extractMessagesArray()
            Log.d(TAG, "pending_messages event received. count=${messagesJson.length()}")
            onPendingMessages(parsePendingMessages(messagesJson))
        }

        socket?.on("new_message") { args ->
            val payload = args.firstOrNull().asJsonObjectOrNull() ?: return@on
            Log.d(TAG, "new_message event received. payload=$payload")
            parseNewMessage(payload)?.let(onNewMessage)
        }

        socket?.on("message_sent") { args ->
            val payload = args.firstOrNull().asJsonObjectOrNull() ?: return@on
            Log.d(TAG, "message_sent event received. payload=$payload")
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
            Log.d(TAG, "user_status event received. payload=$payload")
            onUserStatus(payload.optString("username"), payload.optBoolean("online"))
        }

        socket?.on("message_error") { args ->
            val payload = args.firstOrNull().asJsonObjectOrNull()
            Log.e(TAG, "message_error event received. payload=$payload")
            onMessageError(payload?.optString("error").orEmpty())
        }

        socket?.on(Socket.EVENT_CONNECT_ERROR) { args ->
            val rawError = args.firstOrNull()?.toString().orEmpty()
            Log.e(TAG, "Socket connect error: $rawError")
            onMessageError(rawError.ifBlank { "Socket connection error" })

            if (rawError.contains("Unauthorized", ignoreCase = true) ||
                rawError.contains("401") ||
                rawError.contains("jwt", ignoreCase = true)
            ) {
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
        }

        socket?.on(Socket.EVENT_DISCONNECT) { args ->
            val reason = args.firstOrNull()?.toString().orEmpty()
            Log.d(TAG, "Disconnected from socket. reason=$reason")

            if (reason.equals("io client disconnect", ignoreCase = true)) {
                Log.d(TAG, "disconnect triggered by client, skipping auto reconnect")
                return@on
            }

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
        Log.d(TAG, "Emitting send_message event. payload=$payload")
        socket?.emit("send_message", payload)
    }

    fun getUserStatus(username: String) {
        Log.d(TAG, "Emitting get_user_status for username=$username")
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
        if (isRefreshingSocketToken) {
            Log.d(TAG, "reconnectWithToken: refresh already in progress, skipping duplicate attempt")
            return
        }

        isRefreshingSocketToken = true
        val newToken = callRefreshTokenAPI().normalizeToken()
        try {
            if (newToken.isNotBlank()) {
                Log.d(TAG, "reconnectWithToken: acquired new token, reconnecting socket")
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
            } else {
                Log.e(TAG, "reconnectWithToken: failed to refresh token, socket not reconnected")
            }
        } finally {
            isRefreshingSocketToken = false
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
            Log.d(TAG, "parsePendingMessages[$i]: from=${item.optString("from")}, type=${item.optString("type")}")
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
        if (sender.isBlank()) {
            Log.w(TAG, "parseNewMessage: ignored payload without sender. payload=$payload")
            return null
        }

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

    private fun Any?.extractMessagesArray(): JSONArray {
        return when (this) {
            is JSONArray -> this
            is JSONObject -> optJSONArray("messages") ?: JSONArray()
            is String -> {
                runCatching { JSONArray(this) }.getOrElse {
                    runCatching { JSONObject(this).optJSONArray("messages") ?: JSONArray() }.getOrDefault(JSONArray())
                }
            }
            else -> JSONArray()
        }
    }

    private fun Any?.asJsonObjectOrNull(): JSONObject? {
        return when (this) {
            is JSONObject -> this
            is String -> runCatching { JSONObject(this) }.getOrNull()
            else -> null
        }
    }

    private fun String.normalizeToken(): String =
        trim()
            .removePrefix("Bearer ")
            .removePrefix("bearer ")
            .trim()

    companion object {
        private const val TAG = "SocketManager"
        private const val WebSocketTransport = "websocket"
        private const val MessageTypeText = "text"
    }
}
