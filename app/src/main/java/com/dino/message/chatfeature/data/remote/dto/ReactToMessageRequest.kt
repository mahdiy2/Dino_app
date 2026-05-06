package com.dino.message.chatfeature.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ReactToMessageRequest(
    @SerialName("message_id") val messageId: String,
    val emoji: String? = null,
)
