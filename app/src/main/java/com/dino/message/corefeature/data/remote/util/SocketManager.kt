package com.dino.message.corefeature.data.remote.util

import android.content.Context
import android.util.Log
import androidx.lifecycle.LifecycleCoroutineScope
import com.dino.message.chatfeature.data.remote.dto.MessageSent
import com.dino.message.chatfeature.data.remote.dto.NewMessage
import com.dino.message.chatfeature.data.remote.dto.PendingMessage
import com.dino.message.corefeature.data.remote.ResponseToResultMapper
import com.dino.message.corefeature.data.spref.SPrefManager
import com.dino.message.corefeature.domain.model.asStringResourceContent
import com.dino.message.corefeature.presentation.util.Resource
import com.dino.message.corefeature.util.RetryableException
import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import io.socket.client.IO
import io.socket.client.Socket
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject

class SocketManager @Inject constructor(
    private val sPrefManager: SPrefManager,
    private val context: Context,
    private val httpClient: HttpClient
) {

    private var socket: Socket? = null

    suspend fun connectAndListen(
        onConnected: (String) -> Unit,
        onPendingMessages: (List<PendingMessage>) -> Unit,
        onNewMessage: (NewMessage) -> Unit,
        onMessageSent: (MessageSent) -> Unit,
        onMessageError: (String) -> Unit,
        callRefreshTokenAPI: suspend () -> String
    ) {
        val token = sPrefManager.getToken()
        socket = IO.socket("https://api.dinosocial.ir", IO.Options().apply {
            path = "/socket.io"
            query = "token=$token"
        })

        socket?.on(Socket.EVENT_CONNECT) {
            Log.d("SocketManager", "Connected to socket")
        }

        socket?.on("connected") { args ->
            val username = args[0] as String
            onConnected(username)
        }

        socket?.on("pending_messages") { args ->
            val jsonArray = args[0] as JSONArray
            val messages = mutableListOf<PendingMessage>()

            for (i in 0 until jsonArray.length()) {
                val item = jsonArray.getJSONObject(i)
                messages.add(
                    PendingMessage(
                        from = item.getString("from"),
                        message = item.getString("message"),
                        encryptedKey = item.getString("encrypted_key"),
                        timestamp = item.getString("timestamp")
                    )
                )
            }

            onPendingMessages(messages)
        }

        socket?.on("new_message") { args ->
            val message = JSONObject(args[0].toString()).let {
                NewMessage(
                    from = it.getString("from"),
                    message = it.getString("message"),
                    encryptedKey = it.getString("encrypted_key"),
                    timestamp = it.getString("timestamp")
                )
            }
            onNewMessage(message)
        }

        socket?.on("message_sent") { args ->
            val message = JSONObject(args[0].toString()).let {
                MessageSent(
                    to = it.getString("to"),
                    timestamp = it.getString("timestamp")
                )
            }
            onMessageSent(message)
        }

        socket?.on("message_error") { args ->
            val error = JSONObject(args[0].toString()).getString("error")
            onMessageError(error)
        }

        socket?.on(Socket.EVENT_DISCONNECT) {
            Log.d("SocketManager", "Disconnected from socket")
            CoroutineScope(Dispatchers.IO).launch {
                reconnectWithToken(callRefreshTokenAPI)
            }
        }

        socket?.connect()
    }

    fun sendMessage(
        to: String,
        message: String,
        encryptedKey: String
    ) {
        socket?.emit("send_message", JSONObject().apply {
            put("to", to)
            put("message", message)
            put("encrypted_key", encryptedKey)
        })
    }
    private suspend fun reconnectWithToken(callRefreshTokenAPI: suspend () -> String) {
        val newToken = callRefreshTokenAPI()
        if (newToken.isNotEmpty()) {
            sPrefManager.setToken(newToken)
            connectAndListen(
                onConnected = {},
                onPendingMessages = {},
                onNewMessage = {},
                onMessageSent = {},
                onMessageError = {},
                callRefreshTokenAPI = callRefreshTokenAPI
            )
        }
    }

    fun disconnect() {
        socket?.disconnect()
    }
}