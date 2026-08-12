package com.dino.message.threadfeature.presentation.music

data class MediaQueueItem(
    val trackUrl: String,
    val title: String?,
    val artist: String?
)

typealias PlaybackQueueItem = MediaQueueItem
