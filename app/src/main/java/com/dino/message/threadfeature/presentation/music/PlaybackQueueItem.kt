package com.dino.message.threadfeature.presentation.music

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class PlaybackQueueItem(
    val trackUrl: String,
    val title: String?,
    val artist: String?
) : Parcelable
