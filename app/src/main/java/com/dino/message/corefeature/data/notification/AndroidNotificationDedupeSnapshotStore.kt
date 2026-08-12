package com.dino.message.corefeature.data.notification

import android.content.Context
import com.dino.message.corefeature.domain.notification.NotificationDedupeSnapshotStore
import org.json.JSONObject

class AndroidNotificationDedupeSnapshotStore(
    context: Context,
) : NotificationDedupeSnapshotStore {
    private val prefs = context.getSharedPreferences(PRIVATE_DEDUPE_PREFS, Context.MODE_PRIVATE)

    override fun loadPrivateMessageSnapshot(): Map<String, Long> {
        val rawSnapshot = prefs.getString(PRIVATE_DEDUPE_PREFS_KEY, null) ?: return emptyMap()

        return try {
            val snapshotJson = JSONObject(rawSnapshot)
            val parsed = mutableMapOf<String, Long>()
            val keys = snapshotJson.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                val timestamp = snapshotJson.optLong(key, 0L)
                if (key.isNotBlank() && timestamp > 0L) {
                    parsed[key] = timestamp
                }
            }
            parsed
        } catch (_: Exception) {
            emptyMap()
        }
    }

    override fun savePrivateMessageSnapshot(snapshot: Map<String, Long>) {
        if (snapshot.isEmpty()) {
            prefs.edit().remove(PRIVATE_DEDUPE_PREFS_KEY).apply()
            return
        }

        val snapshotJson = JSONObject()
        snapshot.forEach { (key, timestamp) ->
            snapshotJson.put(key, timestamp)
        }
        prefs.edit().putString(PRIVATE_DEDUPE_PREFS_KEY, snapshotJson.toString()).apply()
    }

    private companion object {
        const val PRIVATE_DEDUPE_PREFS = "private_notification_dedupe"
        const val PRIVATE_DEDUPE_PREFS_KEY = "message_ids"
    }
}
