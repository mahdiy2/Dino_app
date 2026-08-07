package com.dino.message.threadfeature.presentation.music

import kotlinx.coroutines.flow.StateFlow

interface MediaPlaybackController {
    val playbackState: StateFlow<MediaPlaybackState>

    fun toggleTrack(trackUrl: String, title: String?, artist: String?)
    fun playTrack(trackUrl: String, title: String?, artist: String?)
    fun playPlaylist(tracks: List<MediaQueueItem>, startIndex: Int)
    fun pause()
    fun pauseForCompetingAudio()
    fun resume()
    fun seekTo(positionMs: Long)
    fun seekBy(deltaMs: Long)
    fun stop()
    fun setShuffleEnabled(enabled: Boolean)
    fun setRepeatEnabled(enabled: Boolean)
}
