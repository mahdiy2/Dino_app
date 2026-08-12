package com.dino.message.threadfeature.presentation.music

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.StateFlow

object MusicPlaybackManager : MediaPlaybackController {
    override val playbackState: StateFlow<MediaPlaybackState> = MusicPlaybackService.playbackState

    private var appContext: Context? = null

    fun initialize(context: Context) {
        appContext = context.applicationContext
    }

    override fun toggleTrack(
        trackUrl: String,
        title: String?,
        artist: String?,
    ) {
        dispatch(
            action = MusicPlaybackService.ACTION_TOGGLE,
            trackUrl = trackUrl,
            title = title,
            artist = artist,
        )
    }

    override fun playTrack(
        trackUrl: String,
        title: String?,
        artist: String?,
    ) {
        dispatch(
            action = MusicPlaybackService.ACTION_PLAY,
            trackUrl = trackUrl,
            title = title,
            artist = artist,
        )
    }

    override fun playPlaylist(
        tracks: List<MediaQueueItem>,
        startIndex: Int
    ) {
        if (tracks.isEmpty()) return
        val context = requireAppContext() ?: return
        val normalizedIndex = startIndex.coerceIn(0, tracks.lastIndex)
        val queueBundles = ArrayList<Bundle>(tracks.size).apply {
            tracks.forEach { track ->
                add(
                    Bundle().apply {
                        putString(MusicPlaybackService.EXTRA_QUEUE_ITEM_TRACK_URL, track.trackUrl)
                        putString(MusicPlaybackService.EXTRA_QUEUE_ITEM_TITLE, track.title)
                        putString(MusicPlaybackService.EXTRA_QUEUE_ITEM_ARTIST, track.artist)
                    }
                )
            }
        }
        val intent = Intent(context, MusicPlaybackService::class.java).apply {
            action = MusicPlaybackService.ACTION_PLAY_PLAYLIST
            putParcelableArrayListExtra(
                MusicPlaybackService.EXTRA_QUEUE_TRACK_BUNDLES,
                queueBundles
            )
            putExtra(MusicPlaybackService.EXTRA_QUEUE_START_INDEX, normalizedIndex)
        }
        ContextCompat.startForegroundService(context, intent)
    }

    override fun pause() {
        dispatch(action = MusicPlaybackService.ACTION_PAUSE)
    }

    override fun pauseForCompetingAudio() {
        if (!playbackState.value.isPlaying) return
        pause()
    }

    override fun resume() {
        dispatch(action = MusicPlaybackService.ACTION_RESUME)
    }

    override fun seekTo(positionMs: Long) {
        dispatch(
            action = MusicPlaybackService.ACTION_SEEK_TO,
            positionMs = positionMs
        )
    }

    override fun seekBy(deltaMs: Long) {
        dispatch(
            action = MusicPlaybackService.ACTION_SEEK_BY,
            deltaMs = deltaMs
        )
    }

    override fun stop() {
        dispatch(action = MusicPlaybackService.ACTION_STOP)
    }

    override fun setShuffleEnabled(enabled: Boolean) {
        dispatch(
            action = MusicPlaybackService.ACTION_SET_SHUFFLE,
            flagEnabled = enabled
        )
    }

    override fun setRepeatEnabled(enabled: Boolean) {
        dispatch(
            action = MusicPlaybackService.ACTION_SET_REPEAT,
            flagEnabled = enabled
        )
    }

    private fun dispatch(
        action: String,
        trackUrl: String? = null,
        title: String? = null,
        artist: String? = null,
        positionMs: Long? = null,
        deltaMs: Long? = null,
        flagEnabled: Boolean? = null
    ) {
        val context = requireAppContext() ?: return
        val activeTrackUrl = playbackState.value.currentTrackUrl
        val hasActiveTrack = !activeTrackUrl.isNullOrBlank()

        when (action) {
            MusicPlaybackService.ACTION_PLAY -> {
                if (trackUrl.isNullOrBlank()) return
            }
            MusicPlaybackService.ACTION_TOGGLE -> {
                if (trackUrl.isNullOrBlank() && !hasActiveTrack) return
            }
            MusicPlaybackService.ACTION_PAUSE,
            MusicPlaybackService.ACTION_RESUME,
            MusicPlaybackService.ACTION_SEEK_TO,
            MusicPlaybackService.ACTION_SEEK_BY,
            MusicPlaybackService.ACTION_STOP,
            MusicPlaybackService.ACTION_SET_SHUFFLE,
            MusicPlaybackService.ACTION_SET_REPEAT -> {
                if (!hasActiveTrack) return
            }
        }

        val intent = Intent(context, MusicPlaybackService::class.java).apply {
            this.action = action
            if (!trackUrl.isNullOrBlank()) {
                putExtra(MusicPlaybackService.EXTRA_TRACK_URL, trackUrl)
            }
            if (!title.isNullOrBlank()) {
                putExtra(MusicPlaybackService.EXTRA_TITLE, title)
            }
            if (!artist.isNullOrBlank()) {
                putExtra(MusicPlaybackService.EXTRA_ARTIST, artist)
            }
            if (positionMs != null) {
                putExtra(MusicPlaybackService.EXTRA_POSITION_MS, positionMs)
            }
            if (deltaMs != null) {
                putExtra(MusicPlaybackService.EXTRA_DELTA_MS, deltaMs)
            }
            if (flagEnabled != null) {
                putExtra(MusicPlaybackService.EXTRA_ENABLED, flagEnabled)
            }
        }
        val isTogglePauseForCurrentTrack = action == MusicPlaybackService.ACTION_TOGGLE &&
            !trackUrl.isNullOrBlank() &&
            trackUrl == activeTrackUrl &&
            playbackState.value.isPlaying
        val requiresForegroundStart = action == MusicPlaybackService.ACTION_PLAY ||
            action == MusicPlaybackService.ACTION_RESUME ||
            (action == MusicPlaybackService.ACTION_TOGGLE && !isTogglePauseForCurrentTrack)

        if (requiresForegroundStart) {
            ContextCompat.startForegroundService(context, intent)
        } else {
            context.startService(intent)
        }
    }

    private fun requireAppContext(): Context? = appContext
}
