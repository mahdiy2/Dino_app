package com.dino.message.threadfeature.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class CreatePostRequest(
    val text: String,
    val followers_only: Boolean = false,
    val quoted_post_id: Long? = null
)
