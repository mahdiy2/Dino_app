package com.dino.message.reportfeature.data.remote.dto

import com.dino.message.corefeature.data.remote.ResponseToResultMapper
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GetReportTypesResponse(
    @SerialName("report_types")
    val reportTypes: List<String>,
) : ResponseToResultMapper<List<String>> {
    override fun mapResponseToResult(): List<String> = reportTypes
}
