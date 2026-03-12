package com.dino.message.chatfeature.data.remote.dto

data class MessageAttachment(
    val type: String,
    val url: String,
    val mimeType: String,
    val fileName: String? = null,
    val sizeBytes: Long? = null
)
