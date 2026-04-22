package com.dino.message.corefeature.domain.util

object LocalHistoryPaging {
    fun resolveQueryLimit(
        resetPaging: Boolean,
        previouslyLoadedCount: Int,
        previousTotalCount: Int,
        currentTotalCount: Int,
        pageSize: Int,
    ): Int {
        if (currentTotalCount <= 0) return 0

        val safePageSize = pageSize.coerceAtLeast(1)
        val requestedLimit = when {
            resetPaging || previouslyLoadedCount <= 0 -> safePageSize
            previouslyLoadedCount >= previousTotalCount && currentTotalCount > previousTotalCount ->
                previouslyLoadedCount + (currentTotalCount - previousTotalCount)
            else -> previouslyLoadedCount
        }

        return requestedLimit.coerceIn(0, currentTotalCount)
    }
}
