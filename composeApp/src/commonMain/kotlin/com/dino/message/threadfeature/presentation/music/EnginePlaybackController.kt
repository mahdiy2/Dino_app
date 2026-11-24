package com.dino.message.threadfeature.presentation.music

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

interface MediaPlaybackEngine {
    fun load(url: String, onEvent: (PlaybackEngineEvent) -> Unit)
    fun play()
    fun pause()
    fun seekTo(positionMs: Long)
    fun close()
}

sealed interface PlaybackEngineEvent {
    data class Progress(val positionMs: Long, val durationMs: Long) : PlaybackEngineEvent
    data class Status(val playing: Boolean, val buffering: Boolean) : PlaybackEngineEvent
    data class Failure(val message: String) : PlaybackEngineEvent
    data object Ended : PlaybackEngineEvent
}

/** Commands and engine callbacks must run on the same UI thread. */
class EnginePlaybackController(
    private val engine: MediaPlaybackEngine,
    private val queue: PlaybackQueueController = PlaybackQueueController(),
) : MediaPlaybackController {
    private val mutableState = MutableStateFlow(MediaPlaybackState())
    override val playbackState = mutableState.asStateFlow()
    private var generation = 0L
    private var disposed = false
    private var ended = false
    private val state get() = mutableState.value

    override fun toggleTrack(trackUrl: String, title: String?, artist: String?) {
        when {
            state.currentTrackUrl != trackUrl.trim() -> playTrack(trackUrl, title, artist)
            state.isPlaying -> pause()
            else -> resume()
        }
    }

    override fun playTrack(trackUrl: String, title: String?, artist: String?) =
        playPlaylist(listOf(MediaQueueItem(trackUrl, title, artist)), 0)

    override fun playPlaylist(tracks: List<MediaQueueItem>, startIndex: Int) {
        if (disposed) return
        queue.startPlaylist(tracks, startIndex, state.isShuffleEnabled)?.let(::load)
    }

    private fun load(track: MediaQueueItem) {
        ended = false
        val currentGeneration = ++generation
        mutableState.value = state.copy(
            currentTrackUrl = track.trackUrl,
            title = track.title,
            artist = track.artist,
            queue = queue.queue,
            queueIndex = queue.currentQueueIndex,
            isPlaying = false,
            isBuffering = true,
            durationMs = 0,
            positionMs = 0,
            errorMessage = null,
        )
        engine.load(track.trackUrl) { event ->
            if (generation == currentGeneration) onEngineEvent(event)
        }
        if (generation == currentGeneration && state.errorMessage == null) engine.play()
    }

    private fun onEngineEvent(event: PlaybackEngineEvent) {
        when (event) {
            is PlaybackEngineEvent.Progress -> {
                val duration = event.durationMs.coerceAtLeast(0)
                val position = event.positionMs.coerceAtLeast(0)
                mutableState.value = state.copy(
                    durationMs = duration,
                    positionMs = if (duration > 0) position.coerceAtMost(duration) else position,
                )
            }
            is PlaybackEngineEvent.Status -> {
                mutableState.value = state.copy(isPlaying = event.playing, isBuffering = event.buffering)
            }
            is PlaybackEngineEvent.Failure -> {
                mutableState.value = state.copy(
                    isPlaying = false, isBuffering = false, errorMessage = event.message,
                )
            }
            PlaybackEngineEvent.Ended -> {
                val next = queue.nextItemOrNull(state.isRepeatEnabled, state.isShuffleEnabled)
                if (next == null) {
                    ended = true
                    mutableState.value = state.copy(isPlaying = false, isBuffering = false)
                } else {
                    load(next)
                }
            }
        }
    }

    override fun pause() {
        if (state.currentTrackUrl != null) engine.pause()
    }

    override fun pauseForCompetingAudio() = pause()

    override fun resume() {
        if (state.currentTrackUrl == null) return
        if (ended) {
            ended = false
            engine.seekTo(0)
        }
        engine.play()
    }

    override fun seekTo(positionMs: Long) {
        if (state.currentTrackUrl == null) return
        ended = false
        engine.seekTo(positionMs.coerceIn(0, state.durationMs.takeIf { it > 0 } ?: Long.MAX_VALUE))
    }

    override fun seekBy(deltaMs: Long) = seekTo(state.positionMs + deltaMs)

    override fun stop() {
        generation++
        ended = false
        queue.clearQueue()
        mutableState.value = MediaPlaybackState(
            isShuffleEnabled = state.isShuffleEnabled,
            isRepeatEnabled = state.isRepeatEnabled,
        )
        if (!disposed) engine.close()
    }

    fun dispose() {
        if (disposed) return
        stop()
        disposed = true
    }

    override fun setShuffleEnabled(enabled: Boolean) {
        if (state.isShuffleEnabled == enabled) return
        queue.rebuildRemainingOrderFromCurrent(enabled)
        mutableState.value = state.copy(isShuffleEnabled = enabled)
    }

    override fun setRepeatEnabled(enabled: Boolean) {
        mutableState.value = state.copy(isRepeatEnabled = enabled)
    }
}
