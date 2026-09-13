package com.dino.message.threadfeature.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class AddPlaylistTrackRequest(
    val media_id: Long
)
