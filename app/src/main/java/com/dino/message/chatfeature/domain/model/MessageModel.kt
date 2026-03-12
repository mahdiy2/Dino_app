package com.dino.message.chatfeature.domain.model

data class MessageModel(
    val from: String,
    val message: String,
    val encryptedKey: String,
    val timestamp: String
)