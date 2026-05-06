package com.dino.message.chatfeature.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MessageReaction(
    val username: String,
    val emoji: String,
    @SerialName("updated_at") val updatedAt: String? = null,
)
