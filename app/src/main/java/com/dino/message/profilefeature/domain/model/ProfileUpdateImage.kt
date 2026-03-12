package com.dino.message.profilefeature.domain.model

data class ProfileUpdateImage(
    val bytes: ByteArray,
    val fileName: String,
    val mimeType: String
)
