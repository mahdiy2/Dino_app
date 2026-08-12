package com.dino.message.threadfeature.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PlaylistTrack(
    val id: Long,
    @SerialName("media_id")
    val mediaId: Long,
    @SerialName("track_url")
    val trackUrl: String,
    val title: String,
    val artist: String? = null,
    @SerialName("display_name")
    val displayName: String? = null,
    @SerialName("mime_type")
    val mimeType: String,
    @SerialName("added_at")
    val addedAt: String
)

data class PlaylistPage(
    val page: Int,
    val limit: Int,
    val total: Int,
    val tracks: List<PlaylistTrack>
)

data class PlaylistTrackMutation(
    val message: String,
    val created: Boolean,
    val track: PlaylistTrack
)
