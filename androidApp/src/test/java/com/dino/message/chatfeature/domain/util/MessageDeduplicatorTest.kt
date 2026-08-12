package com.dino.message.chatfeature.domain.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

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

        assertFalse("oldest id should have been evicted", deduplicator.isDuplicate("id-0"))
        assertTrue("newest id should remain cached", deduplicator.isDuplicate("latest"))
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
