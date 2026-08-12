package com.dino.message.spashscreen.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class CheckAppVersionRequest(
    val platform: String,
    val version: String,
)
