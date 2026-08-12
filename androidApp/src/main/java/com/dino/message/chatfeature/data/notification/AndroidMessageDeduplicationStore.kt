package com.dino.message.chatfeature.data.notification

import android.content.Context
import com.dino.message.chatfeature.domain.util.MessageDeduplicationStore

class AndroidMessageDeduplicationStore(
    context: Context,
) : MessageDeduplicationStore {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    override fun loadSeenIds(): Set<String> {
        return prefs.getStringSet(KEY_SEEN_IDS, emptySet()) ?: emptySet()
    }

    override fun saveSeenIds(seenIds: Set<String>) {
        prefs.edit().putStringSet(KEY_SEEN_IDS, seenIds).apply()
    }

    private companion object {
        const val PREFS_NAME = "msg_dedup"
        const val KEY_SEEN_IDS = "seen_ids"
    }
}
