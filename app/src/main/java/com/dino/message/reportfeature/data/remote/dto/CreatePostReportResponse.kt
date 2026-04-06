package com.dino.message.reportfeature.data.remote.dto

import com.dino.message.corefeature.data.remote.ResponseToResultMapper
import com.dino.message.corefeature.data.remote.dto.BaseResponse
import com.dino.message.reportfeature.domain.model.CreatedReport
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CreatePostReportResponse(
    @SerialName("report_id")
    val reportId: Long,
    override val message: String,
) : BaseResponse(), ResponseToResultMapper<CreatedReport> {
    override fun mapResponseToResult(): CreatedReport = CreatedReport(
        reportId = reportId,
        message = message,
    )
}
