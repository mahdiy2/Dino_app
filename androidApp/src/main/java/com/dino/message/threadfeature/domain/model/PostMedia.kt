package com.dino.message.threadfeature.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PostMedia(
    val id: Long,
    val url: String,
    val mime_type: String,
    val display_name: String? = null,
    val title: String? = null,
    val artist: String? = null,
    @SerialName("playlist_adders")
    val playlistAdders: List<Author> = emptyList()
)
