package com.dino.message.threadfeature.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class StoryReplyRequest(
    val story_id: Long,
    val reply_text: String
)
