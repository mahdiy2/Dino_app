package com.dino.message.chatfeature.domain.util

import android.content.Context
import android.content.SharedPreferences
import java.util.Collections

class MessageDeduplicator(context: Context) {

    companion object {
        private const val MAX_CACHE_SIZE = 5000
        private const val PREFS_NAME = "msg_dedup"
        private const val KEY_SEEN_IDS = "seen_ids"
    }

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val seenIds: MutableSet<String> = Collections.synchronizedSet(
        LinkedHashSet(loadSeenIds())
    )

    fun isDuplicate(messageId: String?): Boolean {
        if (messageId.isNullOrBlank()) return false
        if (messageId in seenIds) return true
        seenIds.add(messageId)
        if (seenIds.size > MAX_CACHE_SIZE) {
            val iterator = seenIds.iterator()
            iterator.next()
            iterator.remove()
        }
        persistSeenIds()
        return false
    }

    private fun persistSeenIds() {
        prefs.edit().putStringSet(KEY_SEEN_IDS, seenIds.toSet()).apply()
    }

    private fun loadSeenIds(): Set<String> {
        return prefs.getStringSet(KEY_SEEN_IDS, emptySet()) ?: emptySet()
    }
}
