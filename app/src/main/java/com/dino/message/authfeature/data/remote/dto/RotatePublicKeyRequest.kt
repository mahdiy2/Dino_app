package com.dino.message.authfeature.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class RotatePublicKeyRequest(
    val public_key: String,
)
