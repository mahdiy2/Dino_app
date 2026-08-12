package com.dino.message.threadfeature.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class EditPostRequest(
    val text: String,
    val keep_media_ids: List<Long> = emptyList(),
    val followers_only: Boolean? = null
)
