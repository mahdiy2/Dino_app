package com.dino.message.profilefeature.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class UpdateMyProfileRequest(
    val name: String? = null,
    val bio: String? = null
)
