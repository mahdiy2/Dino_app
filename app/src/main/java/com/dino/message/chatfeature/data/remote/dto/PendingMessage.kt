package com.dino.message.chatfeature.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PendingMessage(
     @SerialName("message_id")
     val messageId: String? = null,
     val from: String,
    val type: String,
    val message: String,
    @SerialName("encrypted_key")
    val encryptedKey: String,
    val attachment: MessageAttachment?,
    val timestamp: String
)
