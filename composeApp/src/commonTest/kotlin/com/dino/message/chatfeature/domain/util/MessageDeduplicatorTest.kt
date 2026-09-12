package com.dino.message.chatfeature.domain.util

import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.Test

class MessageDeduplicatorTest {
    @Test
    fun isDuplicatePersistsNewIdsAndLoadsExistingIds() {
        val store = InMemoryMessageDeduplicationStore(setOf("existing"))
        val deduplicator = MessageDeduplicator(store)

        assertTrue(deduplicator.isDuplicate("existing"))
        assertFalse(deduplicator.isDuplicate("new"))
        assertTrue(deduplicator.isDuplicate("new"))
        assertEquals(setOf("existing", "new"), store.savedIds)
    }

    @Test
    fun isDuplicateEvictsOldestIdWhenCacheLimitIsExceeded() {
        val initialIds = (0 until MessageDeduplicator.MAX_CACHE_SIZE).map { "id-$it" }
        val store = InMemoryMessageDeduplicationStore(initialIds.toSet())
        val deduplicator = MessageDeduplicator(store)

        assertFalse(deduplicator.isDuplicate("latest"))

        assertFalse(deduplicator.isDuplicate("id-0"), "oldest id should have been evicted")
        assertTrue(deduplicator.isDuplicate("latest"), "newest id should remain cached")
    }

    @Test
    fun isDuplicateIgnoresBlankIds() {
        val store = InMemoryMessageDeduplicationStore()
        val deduplicator = MessageDeduplicator(store)

        assertFalse(deduplicator.isDuplicate(null))
        assertFalse(deduplicator.isDuplicate(""))
        assertTrue(store.savedIds.isEmpty())
    }

    private class InMemoryMessageDeduplicationStore(
        initialIds: Set<String> = emptySet(),
    ) : MessageDeduplicationStore {
        var savedIds: Set<String> = initialIds
            private set

        override fun loadSeenIds(): Set<String> = savedIds

        override fun saveSeenIds(seenIds: Set<String>) {
            savedIds = seenIds
        }
    }
}
