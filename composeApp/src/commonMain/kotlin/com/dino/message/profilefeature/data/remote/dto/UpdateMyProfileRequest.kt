package com.dino.message.profilefeature.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class UpdateMyProfileRequest(
    val name: String? = null,
    val bio: String? = null,
    val profile_image_shape: String? = null,
    val remove_profile_image: Boolean? = null,
    val remove_profile_video: Boolean? = null,
)
