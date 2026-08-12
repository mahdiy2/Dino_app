package com.dino.message.authfeature.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class RegisterConfirmRequest(
    val registration_id: String,
)
