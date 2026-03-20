package com.dino.message.chatfeature.data.remote.dto

data class NewMessage(
     val messageId: String? = null,
     val from: String,
    val type: String,
    val message: String,
    val encryptedKey: String,
    val attachment: MessageAttachment?,
    val timestamp: String
)
