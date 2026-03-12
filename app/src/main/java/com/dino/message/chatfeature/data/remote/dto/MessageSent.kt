package com.dino.message.chatfeature.data.remote.dto

data class MessageSent(
    val to: String,
    val type: String,
    val timestamp: String
)
