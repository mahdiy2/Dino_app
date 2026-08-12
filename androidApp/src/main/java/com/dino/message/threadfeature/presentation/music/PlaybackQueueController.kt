package com.dino.message.threadfeature.presentation.music

import kotlin.random.Random

class PlaybackQueueController(
    private val randomSeedProvider: () -> Long = { System.currentTimeMillis() }
) {
    var queue: List<MediaQueueItem> = emptyList()
        private set

    var currentQueueIndex: Int = -1
        private set

    private var playbackOrder: List<Int> = emptyList()
    private var currentPlaybackOrderPosition: Int = -1

    fun startPlaylist(
        incomingQueue: List<MediaQueueItem>,
        startIndex: Int,
        isShuffleEnabled: Boolean
    ): MediaQueueItem? {
        val sanitizedQueue = incomingQueue.mapNotNull { queueItem ->
            val normalizedUrl = queueItem.trackUrl.trim()
            if (normalizedUrl.isBlank()) {
                null
            } else {
                queueItem.copy(trackUrl = normalizedUrl)
            }
        }
        if (sanitizedQueue.isEmpty()) return null

        queue = sanitizedQueue
        val normalizedStartIndex = startIndex.coerceIn(0, queue.lastIndex)
        playbackOrder = buildInitialPlaybackOrder(
            startIndex = normalizedStartIndex,
            isShuffleEnabled = isShuffleEnabled,
        )
        currentPlaybackOrderPosition = 0
        return moveToQueueIndex(playbackOrder.firstOrNull() ?: normalizedStartIndex)
    }

    fun clearQueue() {
        queue = emptyList()
        currentQueueIndex = -1
        playbackOrder = emptyList()
        currentPlaybackOrderPosition = -1
    }

    fun moveToQueueIndex(index: Int): MediaQueueItem? {
        if (index !in queue.indices) return null
        currentQueueIndex = index
        return queue[index]
    }

    fun nextItemOrNull(
        isRepeatEnabled: Boolean,
        isShuffleEnabled: Boolean
    ): MediaQueueItem? {
        if (queue.isEmpty() || playbackOrder.isEmpty()) return null

        val nextOrderPosition = currentPlaybackOrderPosition + 1
        if (nextOrderPosition in playbackOrder.indices) {
            currentPlaybackOrderPosition = nextOrderPosition
            return moveToQueueIndex(playbackOrder[nextOrderPosition])
        }

        if (!isRepeatEnabled) return null

        val nextCycleOrder = buildRepeatCycleOrder(isShuffleEnabled)
        if (nextCycleOrder.isEmpty()) return null
        playbackOrder = nextCycleOrder
        currentPlaybackOrderPosition = 0
        return moveToQueueIndex(playbackOrder[0])
    }

    fun rebuildRemainingOrderFromCurrent(isShuffleEnabled: Boolean) {
        if (queue.isEmpty()) return
        if (currentQueueIndex !in queue.indices) return
        if (currentPlaybackOrderPosition !in playbackOrder.indices) return

        val remainingIndices = playbackOrder.drop(currentPlaybackOrderPosition + 1)
        val rebuiltRemaining = if (isShuffleEnabled) {
            remainingIndices.shuffled(Random(randomSeedProvider()))
        } else {
            remainingIndices.sorted()
        }
        playbackOrder = listOf(currentQueueIndex) + rebuiltRemaining
        currentPlaybackOrderPosition = 0
    }

    private fun buildInitialPlaybackOrder(
        startIndex: Int,
        isShuffleEnabled: Boolean
    ): List<Int> {
        if (queue.isEmpty()) return emptyList()
        val validStart = startIndex.coerceIn(0, queue.lastIndex)
        val allIndices = queue.indices.toList()
        return if (isShuffleEnabled) {
            val remaining = allIndices
                .filterNot { it == validStart }
                .shuffled(Random(randomSeedProvider()))
            listOf(validStart) + remaining
        } else {
            val afterStart = ((validStart + 1)..queue.lastIndex).toList()
            val beforeStart = (0 until validStart).toList()
            listOf(validStart) + afterStart + beforeStart
        }
    }

    private fun buildRepeatCycleOrder(isShuffleEnabled: Boolean): List<Int> {
        if (queue.isEmpty()) return emptyList()
        val allIndices = queue.indices.toList()
        return if (isShuffleEnabled) {
            allIndices.shuffled(Random(randomSeedProvider()))
        } else {
            allIndices
        }
    }
}
