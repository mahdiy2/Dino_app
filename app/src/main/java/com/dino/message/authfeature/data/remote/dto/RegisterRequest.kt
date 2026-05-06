package com.dino.message.authfeature.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class RegisterRequest(
    val username: String,
    val password: String,
    val public_key: String,
    val referral_code: String? = null,
    val device_id: String? = null,
)
