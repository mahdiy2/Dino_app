package com.dino.message.chatfeature.domain.util

import java.util.Collections

interface MessageDeduplicationStore {
    fun loadSeenIds(): Set<String>
    fun saveSeenIds(seenIds: Set<String>)
}

class MessageDeduplicator(
    private val store: MessageDeduplicationStore,
) {

    companion object {
        const val MAX_CACHE_SIZE = 5000
    }

    private val seenIds: MutableSet<String> = Collections.synchronizedSet(
        LinkedHashSet(store.loadSeenIds())
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
        store.saveSeenIds(seenIds.toSet())
    }
}
