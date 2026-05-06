package com.dino.message.authfeature.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class RegisterStartRequest(
    val username: String,
    val password: String,
    val public_key: String,
    val client_nonce: String,
    val referral_code: String? = null,
    val device_id: String? = null,
)
