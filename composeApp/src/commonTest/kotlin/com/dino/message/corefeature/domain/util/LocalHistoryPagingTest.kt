package com.dino.message.corefeature.domain.util

import kotlin.test.assertEquals
import kotlin.test.Test

class LocalHistoryPagingTest {

    @Test
    fun resolveQueryLimit_returnsFirstPageOnReset() {
        val queryLimit = LocalHistoryPaging.resolveQueryLimit(
            resetPaging = true,
            previouslyLoadedCount = 250,
            previousTotalCount = 250,
            currentTotalCount = 400,
            pageSize = 100,
        )

        assertEquals(100, queryLimit)
    }

    @Test
    fun resolveQueryLimit_keepsCurrentWindow_whenNotResetAndNoGrowth() {
        val queryLimit = LocalHistoryPaging.resolveQueryLimit(
            resetPaging = false,
            previouslyLoadedCount = 150,
            previousTotalCount = 300,
            currentTotalCount = 300,
            pageSize = 100,
        )

        assertEquals(150, queryLimit)
    }

    @Test
    fun resolveQueryLimit_expandsWindowByDelta_whenNewMessagesArriveAtTail() {
        val queryLimit = LocalHistoryPaging.resolveQueryLimit(
            resetPaging = false,
            previouslyLoadedCount = 200,
            previousTotalCount = 200,
            currentTotalCount = 230,
            pageSize = 100,
        )

        assertEquals(230, queryLimit)
    }

    @Test
    fun resolveQueryLimit_clampsToCurrentTotal_whenRowsWereDeleted() {
        val queryLimit = LocalHistoryPaging.resolveQueryLimit(
            resetPaging = false,
            previouslyLoadedCount = 200,
            previousTotalCount = 200,
            currentTotalCount = 40,
            pageSize = 100,
        )

        assertEquals(40, queryLimit)
    }

    @Test
    fun resolveQueryLimit_returnsZeroWhenNoRowsExist() {
        val queryLimit = LocalHistoryPaging.resolveQueryLimit(
            resetPaging = false,
            previouslyLoadedCount = 0,
            previousTotalCount = 0,
            currentTotalCount = 0,
            pageSize = 100,
        )

        assertEquals(0, queryLimit)
    }
}
