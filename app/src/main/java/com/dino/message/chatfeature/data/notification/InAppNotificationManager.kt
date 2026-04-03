package com.dino.message.chatfeature.data.notification

import android.content.Context
import android.util.Log
import com.dino.message.BuildConfig
import io.socket.client.IO
import io.socket.client.Socket
import org.json.JSONObject

class InAppNotificationManager(
    private val context: Context
) {
    companion object {
        private const val TAG = "InAppNotifManager"
    }

    private var socket: Socket? = null
    private var isConnected = false

    fun connect(token: String) {
        if (isConnected && socket?.connected() == true) {
            Log.d(TAG, "Already connected, skipping")
            return
        }

        disconnect()

        val cleanToken = token.removePrefix("Bearer ").trim()
        if (cleanToken.isBlank()) {
            Log.w(TAG, "Token is blank, cannot connect")
            return
        }

        val baseUrl = BuildConfig.BASE_URL.trimEnd('/')

        val opts = IO.Options.builder()
            .setForceNew(true)
            .setMultiplex(false)
            .setReconnection(true)
            .setReconnectionAttempts(Int.MAX_VALUE)
            .setReconnectionDelay(2_000)
            .setReconnectionDelayMax(15_000)
            .setTimeout(20_000)
            .setPath("/socket.io/")
            .setTransports(arrayOf("websocket", "polling"))
            .setAuth(mapOf("token" to cleanToken))
            .build()

        try {
            val createdSocket = IO.socket(baseUrl, opts)
            registerListeners(createdSocket)
            socket = createdSocket
            createdSocket.connect()
            Log.d(TAG, "Socket connecting to $baseUrl")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create socket", e)
        }
    }

    fun disconnect() {
        socket?.off()
        socket?.disconnect()
        socket = null
        isConnected = false
    }

    private fun registerListeners(createdSocket: Socket) {
        createdSocket.on(Socket.EVENT_CONNECT) {
            isConnected = true
            Log.d(TAG, "Socket connected for notifications")
        }

        createdSocket.on(Socket.EVENT_DISCONNECT) {
            isConnected = false
            Log.d(TAG, "Socket disconnected")
        }

        createdSocket.on(Socket.EVENT_CONNECT_ERROR) { args ->
            Log.w(TAG, "Socket connect error: ${args.firstOrNull()}")
        }

        createdSocket.on("pending_messages") { args ->
            handlePendingMessages(args)
        }

        createdSocket.on("new_notification") { args ->
            handleNewNotification(args)
        }

        createdSocket.on("new_message") { args ->
            handleNewMessage(args)
        }

        createdSocket.on("activity_notification") { args ->
            handleActivityNotification(args)
        }

        createdSocket.on("pending_activity_notifications") { args ->
            handlePendingActivityNotifications(args)
        }
    }

    private fun handlePendingMessages(args: Array<out Any>) {
        try {
            val raw = extractPayloadArg(args)
            val messagesArray = when (raw) {
                is JSONObject -> raw.optJSONArray("messages")
                else -> null
            } ?: return

            val senderCounts = mutableMapOf<String, Int>()
            val senderLastType = mutableMapOf<String, String>()

            for (i in 0 until messagesArray.length()) {
                val msg = messagesArray.optJSONObject(i) ?: continue
                val sender = msg.optString("from", "")
                if (sender.isNotBlank()) {
                    senderCounts[sender] = (senderCounts[sender] ?: 0) + 1
                    senderLastType[sender] = msg.optString("type", "text")
                }
            }

            for ((sender, count) in senderCounts) {
                if (ActiveChatTracker.isChattingWith(sender)) continue

                val lastType = senderLastType[sender] ?: "text"
                val body = buildNotificationBody(lastType, count)
                NotificationHelper.showMessageNotification(context, sender, body)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error handling pending_messages", e)
        }
    }

    private fun handleNewNotification(args: Array<out Any>) {
        try {
            val raw = extractPayloadArg(args)
            val payload = when (raw) {
                is JSONObject -> raw
                is String -> try { JSONObject(raw) } catch (_: Exception) { null }
                else -> null
            } ?: return

            val sender = payload.optString("from", "")
            if (sender.isBlank()) return

            if (ActiveChatTracker.isChattingWith(sender)) return

            val type = payload.optString("type", "text")
            val body = buildNotificationBody(type, 1)
            NotificationHelper.showMessageNotification(context, sender, body)
        } catch (e: Exception) {
            Log.e(TAG, "Error handling new_notification", e)
        }
    }

    private fun handleNewMessage(args: Array<out Any>) {
        try {
            val raw = extractPayloadArg(args)
            val payload = when (raw) {
                is JSONObject -> raw
                is String -> try { JSONObject(raw) } catch (_: Exception) { null }
                else -> null
            } ?: return

            val sender = payload.optString("from", "")
            if (sender.isBlank()) return

            if (ActiveChatTracker.isChattingWith(sender)) return

            val type = payload.optString("type", "text")
            val body = buildNotificationBody(type, 1)
            NotificationHelper.showMessageNotification(context, sender, body)
        } catch (e: Exception) {
            Log.e(TAG, "Error handling new_message for notification", e)
        }
    }

    private fun buildNotificationBody(type: String, count: Int): String {
        return when (type) {
            "image" -> "Sent you an image"
            "video" -> "Sent you a video"
            "voice" -> "Sent you a voice message"
            "file" -> "Sent you a file"
            "mixed" -> "Sent you a message with attachment"
            else -> if (count > 1) "$count new messages" else "Sent you a message"
        }
    }

    private fun extractPayloadArg(args: Array<out Any>): Any? {
        for (arg in args) {
            when (arg) {
                is JSONObject -> return arg
                is String -> {
                    val trimmed = arg.trim()
                    if (trimmed.startsWith("{") || trimmed.startsWith("[")) {
                        return try { JSONObject(trimmed) } catch (_: Exception) { arg }
                    }
                }
                else -> return arg
            }
        }
        return null
    }

    private fun handleActivityNotification(args: Array<out Any>) {
        try {
            val raw = extractPayloadArg(args)
            val payload = when (raw) {
                is JSONObject -> raw
                is String -> try { JSONObject(raw) } catch (_: Exception) { null }
                else -> null
            } ?: return

            val kind = payload.optString("kind", "")
            val actorObj = payload.optJSONObject("actor")
            val actorName = actorObj?.optString("name", null)
                ?: actorObj?.optString("username", "Someone")
                ?: "Someone"

            val body = buildActivityNotificationBody(kind, actorName, payload)
            val notifId = payload.optLong("id", System.currentTimeMillis()).toInt()
            NotificationHelper.showActivityNotification(context, "Activity", body, notifId)
        } catch (e: Exception) {
            Log.e(TAG, "Error handling activity_notification", e)
        }
    }

    private fun handlePendingActivityNotifications(args: Array<out Any>) {
        try {
            val raw = extractPayloadArg(args)
            val payload = when (raw) {
                is JSONObject -> raw
                is String -> try { JSONObject(raw) } catch (_: Exception) { null }
                else -> null
            } ?: return

            val unreadCount = payload.optInt("unread_count", 0)
            if (unreadCount <= 0) return

            val notifications = payload.optJSONArray("notifications")
            if (notifications != null && notifications.length() > 0) {
                val first = notifications.optJSONObject(0)
                if (first != null) {
                    val kind = first.optString("kind", "")
                    val actorObj = first.optJSONObject("actor")
                    val actorName = actorObj?.optString("name", null)
                        ?: actorObj?.optString("username", "Someone")
                        ?: "Someone"

                    val body = if (unreadCount == 1) {
                        buildActivityNotificationBody(kind, actorName, first)
                    } else {
                        "You have $unreadCount new activity notifications"
                    }
                    NotificationHelper.showActivityNotification(context, "Activity", body)
                }
            } else {
                NotificationHelper.showActivityNotification(
                    context, "Activity",
                    "You have $unreadCount new activity notifications"
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error handling pending_activity_notifications", e)
        }
    }

    private fun buildActivityNotificationBody(kind: String, actorName: String, payload: JSONObject): String {
        val extra = payload.optJSONObject("extra")
        return when (kind) {
            "follow" -> "$actorName started following you"
            "unfollow" -> "$actorName unfollowed you"
            "comment" -> {
                val preview = extra?.optString("comment_preview", "") ?: ""
                if (preview.isNotBlank()) "$actorName commented: $preview"
                else "$actorName commented on your post"
            }
            "vote" -> {
                val label = extra?.optString("vote_label", "voted") ?: "voted"
                "$actorName ${label}d your post"
            }
            else -> "$actorName interacted with your content"
        }
    }
}
