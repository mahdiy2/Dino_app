package com.dino.message.profilefeature.data.remote.dto

import com.dino.message.corefeature.data.remote.ResponseToResultMapper
import com.dino.message.profilefeature.domain.model.Profile
import kotlinx.serialization.Serializable

@Serializable
data class ProfileResponse(
    val username: String,
    val name: String? = null,
    val bio: String? = null,
    val profile_image_url: String? = null,
    val profile_video_url: String? = null,
    val followers_count: Long = 0,
    val following_count: Long = 0,
    val posts_count: Long = 0
) : ResponseToResultMapper<Profile> {
    override fun mapResponseToResult(): Profile = Profile(
        username = username,
        name = name,
        bio = bio,
        profile_image_url = profile_image_url,
        profile_video_url = profile_video_url,
        followers_count = followers_count,
        following_count = following_count,
        posts_count = posts_count
    )
}
