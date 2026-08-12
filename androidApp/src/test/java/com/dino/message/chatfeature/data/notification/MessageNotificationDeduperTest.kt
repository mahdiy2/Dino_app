package com.dino.message.chatfeature.data.notification

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MessageNotificationDeduperTest {

    @Test
    fun shouldNotify_blocksDuplicateMessageIdWithinTtl() {
        var now = 1_000L
        val deduper = MessageNotificationDeduper(
            ttlMs = 60_000L,
            maxEntries = 100,
            clock = { now },
        )

        assertTrue(deduper.shouldNotify("private", "msg-1"))
        now += 5_000L
        assertFalse(deduper.shouldNotify("private", "msg-1"))
    }

    @Test
    fun shouldNotify_allowsSameMessageIdAcrossDifferentChannels() {
        val deduper = MessageNotificationDeduper(
            ttlMs = 60_000L,
            maxEntries = 100,
            clock = { 1_000L },
        )

        assertTrue(deduper.shouldNotify("private", "msg-1"))
        assertTrue(deduper.shouldNotify("group", "msg-1"))
    }

    @Test
    fun shouldNotify_allowsMessageIdAgainAfterTtlExpiry() {
        var now = 1_000L
        val deduper = MessageNotificationDeduper(
            ttlMs = 30_000L,
            maxEntries = 100,
            clock = { now },
        )

        assertTrue(deduper.shouldNotify("private", "msg-1"))
        now += 31_000L
        assertTrue(deduper.shouldNotify("private", "msg-1"))
    }

    @Test
    fun shouldNotify_restoresStateFromSnapshotAcrossInstances() {
        var now = 1_000L
        var persisted = emptyMap<String, Long>()
        val saveSnapshot: (Map<String, Long>) -> Unit = { snapshot ->
            persisted = snapshot.toMap()
        }

        val firstInstance = MessageNotificationDeduper(
            ttlMs = 60_000L,
            maxEntries = 100,
            clock = { now },
            snapshotLoader = { persisted },
            snapshotSaver = saveSnapshot,
        )
        assertTrue(firstInstance.shouldNotify("private", "msg-1"))

        now += 5_000L
        val secondInstance = MessageNotificationDeduper(
            ttlMs = 60_000L,
            maxEntries = 100,
            clock = { now },
            snapshotLoader = { persisted },
            snapshotSaver = saveSnapshot,
        )
        assertFalse(secondInstance.shouldNotify("private", "msg-1"))
    }
}
