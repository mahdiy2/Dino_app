package com.dino.message.profilefeature.domain.model

data class Profile(
    val username: String,
    val name: String?,
    val badge: String? = null,
    val referral_code: String? = null,
    val bio: String?,
    val profile_image_url: String?,
    val profile_image_shape: String? = null,
    val profile_video_url: String?,
    val followers_count: Long,
    val following_count: Long,
    val posts_count: Long
)
