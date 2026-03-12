package com.dino.message.profilefeature.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class ProfileUserSummary(
    val id: Long,
    val username: String,
    val name: String? = null,
    val profile_image_url: String? = null
)
