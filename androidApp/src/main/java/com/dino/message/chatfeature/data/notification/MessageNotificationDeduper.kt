package com.dino.message.chatfeature.data.notification

import java.util.Locale

class MessageNotificationDeduper(
    private val ttlMs: Long,
    private val maxEntries: Int,
    private val clock: () -> Long = { System.currentTimeMillis() },
    private val snapshotLoader: () -> Map<String, Long> = { emptyMap() },
    private val snapshotSaver: (Map<String, Long>) -> Unit = {},
) {
    private val lock = Any()
    private var loaded = false
    private val recentByKey = linkedMapOf<String, Long>()

    fun shouldNotify(channel: String, messageId: String?): Boolean {
        val dedupeKey = normalizeKey(channel, messageId) ?: return true
        synchronized(lock) {
            ensureLoadedLocked()
            val now = clock()
            pruneExpiredLocked(now)

            val seenAt = recentByKey[dedupeKey]
            if (seenAt != null && now - seenAt <= ttlMs) {
                return false
            }

            recentByKey.remove(dedupeKey)
            recentByKey[dedupeKey] = now
            trimOverflowLocked()
            persistLocked()
            return true
        }
    }

    fun clear() {
        synchronized(lock) {
            loaded = true
            recentByKey.clear()
            persistLocked()
        }
    }

    private fun ensureLoadedLocked() {
        if (loaded) return
        loaded = true

        val now = clock()
        val loadedSnapshot = snapshotLoader()
        for ((rawKey, seenAt) in loadedSnapshot) {
            val normalizedKey = normalizePersistedKey(rawKey) ?: continue
            if (seenAt <= 0L) continue
            if (now - seenAt > ttlMs) continue
            recentByKey[normalizedKey] = seenAt
        }
        trimOverflowLocked()
    }

    private fun pruneExpiredLocked(now: Long) {
        val iterator = recentByKey.entries.iterator()
        var changed = false
        while (iterator.hasNext()) {
            val entry = iterator.next()
            if (now - entry.value > ttlMs) {
                iterator.remove()
                changed = true
            }
        }
        if (changed) {
            persistLocked()
        }
    }

    private fun trimOverflowLocked() {
        if (maxEntries <= 0 || recentByKey.size <= maxEntries) return

        val overflow = recentByKey.size - maxEntries
        val oldestKeys = recentByKey.entries
            .sortedBy { it.value }
            .take(overflow)
            .map { it.key }
        for (key in oldestKeys) {
            recentByKey.remove(key)
        }
    }

    private fun persistLocked() {
        try {
            snapshotSaver(recentByKey.toMap())
        } catch (_: Exception) {
            // Dedupe persistence is best-effort; runtime dedupe still remains active.
        }
    }

    private fun normalizeKey(channel: String, messageId: String?): String? {
        val normalizedChannel = channel.trim().lowercase(Locale.US)
        val normalizedMessageId = messageId?.trim().orEmpty()
        if (normalizedChannel.isBlank() || normalizedMessageId.isBlank()) {
            return null
        }
        return "$normalizedChannel:$normalizedMessageId"
    }

    private fun normalizePersistedKey(rawKey: String): String? {
        if (rawKey.isBlank()) return null
        val separatorIndex = rawKey.indexOf(':')
        if (separatorIndex <= 0 || separatorIndex >= rawKey.length - 1) return null
        val channel = rawKey.substring(0, separatorIndex)
        val messageId = rawKey.substring(separatorIndex + 1)
        return normalizeKey(channel, messageId)
    }
}
