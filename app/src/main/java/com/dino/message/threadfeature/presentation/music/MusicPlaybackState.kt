package com.dino.message.threadfeature.presentation.music

data class MediaPlaybackState(
    val currentTrackUrl: String? = null,
    val title: String? = null,
    val artist: String? = null,
    val queue: List<MediaQueueItem> = emptyList(),
    val queueIndex: Int = -1,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val durationMs: Long = 0L,
    val positionMs: Long = 0L,
    val errorMessage: String? = null,
    val isShuffleEnabled: Boolean = false,
    val isRepeatEnabled: Boolean = false
)

typealias MusicPlaybackUiState = MediaPlaybackState
