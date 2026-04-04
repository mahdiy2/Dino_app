package com.dino.message.authfeature.domain.model

data class RegisterStartUser(
    val registrationId: String,
    val expiresInSeconds: Int,
)
