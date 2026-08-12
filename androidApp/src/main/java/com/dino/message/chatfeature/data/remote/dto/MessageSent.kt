package com.dino.message.chatfeature.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MessageSent(
     val to: String,
     @SerialName("message_id")
     val messageId: String? = null,
     val type: String,
    val timestamp: String
)
