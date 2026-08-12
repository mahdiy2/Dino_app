package com.dino.message.threadfeature.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class StoryLikeRequest(
    val story_id: Long,
    val liked: Boolean
)
