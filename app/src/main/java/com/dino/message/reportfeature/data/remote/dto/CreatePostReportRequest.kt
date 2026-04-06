package com.dino.message.reportfeature.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CreatePostReportRequest(
    @SerialName("report_type")
    val reportType: String,
    val description: String? = null,
)
