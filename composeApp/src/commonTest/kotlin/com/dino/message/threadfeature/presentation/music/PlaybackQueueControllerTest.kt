package com.dino.message.threadfeature.presentation.music

import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.Test

class PlaybackQueueControllerTest {

    @Test
    fun `startPlaylist ignores empty and blank queues`() {
        val controller = PlaybackQueueController()

        assertNull(controller.startPlaylist(emptyList(), startIndex = 0, isShuffleEnabled = false))
        assertNull(
            controller.startPlaylist(
                incomingQueue = listOf(queueItem("   ")),
                startIndex = 0,
                isShuffleEnabled = false,
            )
        )
        assertTrue(controller.queue.isEmpty())
        assertEquals(-1, controller.currentQueueIndex)
    }

    @Test
    fun `nextItemOrNull returns null when no playlist is active`() {
        val controller = PlaybackQueueController()

        assertNull(controller.nextItemOrNull(isRepeatEnabled = true, isShuffleEnabled = true))
    }

    @Test
    fun `startPlaylist trims urls and clamps start index`() {
        val controller = PlaybackQueueController()

        val first = controller.startPlaylist(
            incomingQueue = listOf(queueItem(" one "), queueItem("two")),
            startIndex = 99,
            isShuffleEnabled = false,
        )

        assertEquals("two", first?.trackUrl)
        assertEquals(1, controller.currentQueueIndex)
        assertEquals(listOf("one", "two"), controller.queue.map { it.trackUrl })
    }

    @Test
    fun `sequential order continues from start and wraps before stopping`() {
        val controller = PlaybackQueueController()

        assertEquals(
            "two",
            controller.startPlaylist(queue(), startIndex = 1, isShuffleEnabled = false)?.trackUrl,
        )

        assertEquals("three", controller.nextItemOrNull(isRepeatEnabled = false, isShuffleEnabled = false)?.trackUrl)
        assertEquals("one", controller.nextItemOrNull(isRepeatEnabled = false, isShuffleEnabled = false)?.trackUrl)
        assertNull(controller.nextItemOrNull(isRepeatEnabled = false, isShuffleEnabled = false))
    }

    @Test
    fun `shuffle keeps selected start track first`() {
        val controller = PlaybackQueueController(randomSeedProvider = { 7L })

        val first = controller.startPlaylist(
            incomingQueue = queue(),
            startIndex = 1,
            isShuffleEnabled = true,
        )

        assertEquals("two", first?.trackUrl)
        assertEquals(1, controller.currentQueueIndex)
    }

    @Test
    fun `repeat starts a new cycle when queue finishes`() {
        val controller = PlaybackQueueController()

        assertEquals("two", controller.startPlaylist(queue(), startIndex = 1, isShuffleEnabled = false)?.trackUrl)
        assertEquals("three", controller.nextItemOrNull(isRepeatEnabled = true, isShuffleEnabled = false)?.trackUrl)
        assertEquals("one", controller.nextItemOrNull(isRepeatEnabled = true, isShuffleEnabled = false)?.trackUrl)
        assertEquals("one", controller.nextItemOrNull(isRepeatEnabled = true, isShuffleEnabled = false)?.trackUrl)
    }

    @Test
    fun `rebuildRemainingOrderFromCurrent keeps current track and sorts remaining when shuffle is disabled`() {
        val controller = PlaybackQueueController(randomSeedProvider = { 3L })

        assertEquals("two", controller.startPlaylist(queue(), startIndex = 1, isShuffleEnabled = true)?.trackUrl)
        controller.rebuildRemainingOrderFromCurrent(isShuffleEnabled = false)

        assertEquals("one", controller.nextItemOrNull(isRepeatEnabled = false, isShuffleEnabled = false)?.trackUrl)
        assertEquals("three", controller.nextItemOrNull(isRepeatEnabled = false, isShuffleEnabled = false)?.trackUrl)
    }

    private fun queue(): List<MediaQueueItem> {
        return listOf(
            queueItem("one"),
            queueItem("two"),
            queueItem("three"),
        )
    }

    private fun queueItem(trackUrl: String): MediaQueueItem {
        return MediaQueueItem(
            trackUrl = trackUrl,
            title = "Track $trackUrl",
            artist = "Artist",
        )
    }
}
