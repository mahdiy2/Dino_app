package com.dino.message.chatfeature.data.remote.dto

data class PendingMessage(
    val from: String,
    val message: String,
    val encryptedKey: String,
    val timestamp: String
)