package com.dino.message.corefeature.data.background

import android.content.Context
import android.content.SharedPreferences

class NotificationDedupStore(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    fun shouldNotifyActivity(latestNotificationId: Long): Boolean {
        val lastNotifiedId = prefs.getLong(KEY_LAST_ACTIVITY_ID, 0L)
        return latestNotificationId > lastNotifiedId
    }

    fun markActivityNotified(latestNotificationId: Long) {
        prefs.edit().putLong(KEY_LAST_ACTIVITY_ID, latestNotificationId).apply()
    }

    fun shouldNotifyMessage(sender: String, lastTimestamp: String): Boolean {
        val key = messageTimestampKey(sender)
        val lastNotifiedTimestamp = prefs.getString(key, null)
        return lastNotifiedTimestamp != lastTimestamp
    }

    fun markMessageNotified(sender: String, lastTimestamp: String) {
        prefs.edit().putString(messageTimestampKey(sender), lastTimestamp).apply()
    }

    fun shouldNotifyGroup(groupId: Int, lastTimestamp: String): Boolean {
        val key = groupTimestampKey(groupId)
        val lastNotifiedTimestamp = prefs.getString(key, null)
        return lastNotifiedTimestamp != lastTimestamp
    }

    fun markGroupNotified(groupId: Int, lastTimestamp: String) {
        prefs.edit().putString(groupTimestampKey(groupId), lastTimestamp).apply()
    }

    private fun messageTimestampKey(sender: String): String = "$KEY_MESSAGE_PREFIX$sender"
    private fun groupTimestampKey(groupId: Int): String = "$KEY_GROUP_PREFIX$groupId"

    companion object {
        private const val PREF_NAME = "notification_dedup_store"
        private const val KEY_LAST_ACTIVITY_ID = "last_activity_id"
        private const val KEY_MESSAGE_PREFIX = "last_message_ts_"
        private const val KEY_GROUP_PREFIX = "last_group_message_ts_"
    }
}
