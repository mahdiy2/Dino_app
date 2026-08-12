package com.dino.message.chatfeature.data.notification

import com.dino.message.corefeature.data.remote.logging.AppLogger
import com.dino.message.corefeature.data.socket.AppSocketManager
import com.dino.message.corefeature.data.socket.RealtimeSocketEvents
import com.dino.message.corefeature.domain.notification.AppNotificationPresenter
import com.dino.message.corefeature.domain.notification.NotificationDedupeSnapshotStore
import com.dino.message.corefeature.domain.notification.NotificationSocketConnection
import com.dino.message.corefeature.domain.system.AppClock
import org.json.JSONObject

class InAppNotificationManager constructor(
    private val appSocketManager: AppSocketManager,
    private val notificationPresenter: AppNotificationPresenter,
    private val dedupeSnapshotStore: NotificationDedupeSnapshotStore,
    private val appClock: AppClock,
    private val appLogger: AppLogger,
) : NotificationSocketConnection {
    companion object {
        private const val TAG = "InAppNotifManager"
        private const val LISTENER_OWNER = "InAppNotificationManager"
        private const val PRIVATE_NOTIFICATION_CHANNEL = "private"
        private const val PRIVATE_DEDUPE_TTL_MS = 30 * 60 * 1000L
        private const val PRIVATE_DEDUPE_MAX_ENTRIES = 4_000
    }

    private val privateMessageDeduper = MessageNotificationDeduper(
        ttlMs = PRIVATE_DEDUPE_TTL_MS,
        maxEntries = PRIVATE_DEDUPE_MAX_ENTRIES,
        snapshotLoader = dedupeSnapshotStore::loadPrivateMessageSnapshot,
        snapshotSaver = dedupeSnapshotStore::savePrivateMessageSnapshot,
    )

    override fun connect(token: String) {
        val cleanToken = token.removePrefix("Bearer ").trim()
        if (cleanToken.isBlank()) {
            appLogger.warning(TAG, "Token is blank, cannot connect")
            return
        }
        registerListeners()
        appSocketManager.connect()
    }

    override fun disconnect() {
        appSocketManager.disconnect(clearListeners = false)
    }

    fun clearPrivateMessageNotificationDedupe() {
        privateMessageDeduper.clear()
    }

    private fun registerListeners() {
        appSocketManager.registerListener(LISTENER_OWNER, RealtimeSocketEvents.CONNECT) {
            appLogger.debug(TAG, "Socket connected for notifications")
        }

        appSocketManager.registerListener(LISTENER_OWNER, RealtimeSocketEvents.DISCONNECT) {
            appLogger.debug(TAG, "Socket disconnected")
        }

        appSocketManager.registerListener(LISTENER_OWNER, RealtimeSocketEvents.CONNECT_ERROR) { args ->
            appLogger.warning(TAG, "Socket connect error: ${args.firstOrNull()}")
        }

        appSocketManager.registerListener(LISTENER_OWNER, "pending_messages") { args ->
            handlePendingMessages(args)
        }

        appSocketManager.registerListener(LISTENER_OWNER, "new_notification") { args ->
            handleNewNotification(args)
        }

        appSocketManager.registerListener(LISTENER_OWNER, "new_message") { args ->
            handleNewMessage(args)
        }

        appSocketManager.registerListener(LISTENER_OWNER, "pending_group_messages") { args ->
            handlePendingGroupMessages(args)
        }

        appSocketManager.registerListener(LISTENER_OWNER, "activity_notification") { args ->
            handleActivityNotification(args)
        }

        appSocketManager.registerListener(LISTENER_OWNER, "pending_activity_notifications") { args ->
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
                    val messageId = msg.optString("message_id", "").trim()
                    if (!privateMessageDeduper.shouldNotify(PRIVATE_NOTIFICATION_CHANNEL, messageId)) {
                        continue
                    }
                    senderCounts[sender] = (senderCounts[sender] ?: 0) + 1
                    senderLastType[sender] = msg.optString("type", "text")
                }
            }

            for ((sender, count) in senderCounts) {
                if (ActiveChatTracker.isChattingWith(sender)) continue

                val lastType = senderLastType[sender] ?: "text"
                val body = buildNotificationBody(lastType, count)
                notificationPresenter.showPrivateMessage(sender, body)
            }
        } catch (e: Exception) {
            appLogger.error(TAG, "Error handling pending_messages", e)
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

            val groupId = payload.optInt("group_id", -1)
            if (groupId > 0) {
                if (ActiveGroupTracker.isViewingGroup(groupId)) return

                val groupName = payload.optString("group_name", "Group Chat")
                val type = payload.optString("type", "text")
                val body = buildGroupNotificationBody(sender, type)
                notificationPresenter.showGroupMessage(
                    groupId = groupId,
                    groupName = groupName,
                    body = body
                )
                return
            }
            // Deterministic overlap policy: private notification UI is driven by `new_message`.
            // This keeps behavior stable when legacy sockets emit both `new_notification` and `new_message`.
            return
        } catch (e: Exception) {
            appLogger.error(TAG, "Error handling new_notification", e)
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

            val messageId = payload.optString("message_id", "").trim()
            if (!privateMessageDeduper.shouldNotify(PRIVATE_NOTIFICATION_CHANNEL, messageId)) {
                return
            }
            if (ActiveChatTracker.isChattingWith(sender)) return

            val type = payload.optString("type", "text")
            val body = buildNotificationBody(type, 1)
            notificationPresenter.showPrivateMessage(sender, body)
        } catch (e: Exception) {
            appLogger.error(TAG, "Error handling new_message for notification", e)
        }
    }

    private fun buildNotificationBody(type: String, count: Int): String {
        return when (type) {
            "image" -> "Sent you an image"
            "video" -> "Sent you a video"
            "voice" -> "Sent you a voice message"
            "file" -> "Sent you a file"
            "mixed" -> "Sent you a message with attachment"
            "post" -> "Shared a post with you"
            else -> if (count > 1) "$count new messages" else "Sent you a message"
        }
    }

    private fun buildGroupNotificationBody(sender: String, type: String): String {
        return when (type) {
            "image" -> "$sender sent an image"
            "video" -> "$sender sent a video"
            "voice" -> "$sender sent a voice message"
            "file" -> "$sender sent a file"
            "mixed" -> "$sender sent a message with attachment"
            "post" -> "$sender shared a post"
            else -> "$sender sent a message"
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
            val actorName = actorObj?.optString("name").orEmpty()
                .ifBlank { actorObj?.optString("username").orEmpty() }
                .ifBlank { "Someone" }

            val body = buildActivityNotificationBody(kind, actorName, payload)
            val notifId = payload.optLong("id", appClock.currentTimeMillis()).toInt()
            notificationPresenter.showActivity("Activity", body, notifId)
        } catch (e: Exception) {
            appLogger.error(TAG, "Error handling activity_notification", e)
        }
    }

    private fun handlePendingGroupMessages(args: Array<out Any>) {
        try {
            val raw = extractPayloadArg(args)
            val payload = when (raw) {
                is JSONObject -> raw
                is String -> try { JSONObject(raw) } catch (_: Exception) { null }
                else -> null
            } ?: return

            val groupId = payload.optInt("group_id", -1)
            if (groupId <= 0) return
            if (ActiveGroupTracker.isViewingGroup(groupId)) return

            val groupName = payload.optString("group_name", "Group Chat")
            val messages = payload.optJSONArray("messages") ?: return
            if (messages.length() <= 0) return

            val latest = messages.optJSONObject(messages.length() - 1)
            val sender = latest?.optString("from", "Someone") ?: "Someone"
            val type = latest?.optString("type", "text") ?: "text"
            val body = if (messages.length() > 1) {
                "${messages.length()} new messages"
            } else {
                buildGroupNotificationBody(sender, type)
            }

            notificationPresenter.showGroupMessage(
                groupId = groupId,
                groupName = groupName,
                body = body
            )
        } catch (e: Exception) {
            appLogger.error(TAG, "Error handling pending_group_messages", e)
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
                    val actorName = actorObj?.optString("name").orEmpty()
                        .ifBlank { actorObj?.optString("username").orEmpty() }
                        .ifBlank { "Someone" }

                    val body = if (unreadCount == 1) {
                        buildActivityNotificationBody(kind, actorName, first)
                    } else {
                        "You have $unreadCount new activity notifications"
                    }
                    notificationPresenter.showActivity("Activity", body)
                }
            } else {
                notificationPresenter.showActivity("Activity", "You have $unreadCount new activity notifications")
            }
        } catch (e: Exception) {
            appLogger.error(TAG, "Error handling pending_activity_notifications", e)
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
