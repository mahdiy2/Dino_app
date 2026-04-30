package com.dino.message.threadfeature.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class Author(
    val id: Long,
    val username: String,
    val name: String?,
    val profile_image_url: String?,
    val profile_image_shape: String? = null,
    val badge: String? = null,
)
