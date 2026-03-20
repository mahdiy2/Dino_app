package com.dino.message.searchfeature.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class SearchUser(
    val id: Long,
    val username: String,
    val name: String?,
    val profile_image_url: String?
)
