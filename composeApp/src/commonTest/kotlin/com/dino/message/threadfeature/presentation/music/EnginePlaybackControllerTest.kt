package com.dino.message.threadfeature.presentation.music

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class EnginePlaybackControllerTest {
    @Test
    fun advancesAndRepeatsWithoutApplyingStaleEngineEvents() {
        val engine = FakeEngine()
        val controller = EnginePlaybackController(engine)
        controller.playPlaylist(
            listOf(MediaQueueItem(" first ", "First", null), MediaQueueItem("second", "Second", null)),
            0,
        )
        val first = engine.callbacks.single()
        assertEquals("first", controller.playbackState.value.currentTrackUrl)

        first(PlaybackEngineEvent.Ended)
        val second = engine.callbacks.last()
        first(PlaybackEngineEvent.Failure("late callback"))
        assertEquals(null, controller.playbackState.value.errorMessage)
        assertEquals(1, controller.playbackState.value.queueIndex)

        second(PlaybackEngineEvent.Progress(1_200, 1_000))
        assertEquals(1_000, controller.playbackState.value.positionMs)
        second(PlaybackEngineEvent.Status(playing = true, buffering = false))
        assertTrue(controller.playbackState.value.isPlaying)

        controller.setRepeatEnabled(true)
        second(PlaybackEngineEvent.Ended)
        assertEquals("first", controller.playbackState.value.currentTrackUrl)

        controller.dispose()
        engine.callbacks.last()(PlaybackEngineEvent.Status(playing = true, buffering = false))
        assertFalse(controller.playbackState.value.isPlaying)
        assertEquals(-1, controller.playbackState.value.queueIndex)
    }

    @Test
    fun resumeAfterLastTrackEndsSeeksToStart() {
        val engine = FakeEngine()
        val controller = EnginePlaybackController(engine)
        controller.playTrack("track", "Title", null)
        engine.callbacks.single()(PlaybackEngineEvent.Ended)
        controller.resume()
        assertEquals(listOf(0L), engine.seeks)
    }

    private class FakeEngine : MediaPlaybackEngine {
        val seeks = mutableListOf<Long>()
        val callbacks = mutableListOf<(PlaybackEngineEvent) -> Unit>()
        override fun load(url: String, onEvent: (PlaybackEngineEvent) -> Unit) {
            callbacks += onEvent
        }
        override fun play() = Unit
        override fun pause() = Unit
        override fun seekTo(positionMs: Long) { seeks += positionMs }
        override fun close() = Unit
    }
}
