package com.dino.message.authfeature.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class UpdateDeviceIdRequest(
    val device_id: String,
)
