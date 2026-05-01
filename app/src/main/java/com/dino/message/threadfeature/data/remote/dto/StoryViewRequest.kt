package com.dino.message.threadfeature.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class StoryViewRequest(
    val story_id: Long
)
