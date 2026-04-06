package com.dino.message.reportfeature.data.repository

import com.dino.message.corefeature.presentation.util.Resource
import com.dino.message.reportfeature.data.remote.ReportApiService
import com.dino.message.reportfeature.domain.model.CreatedReport
import com.dino.message.reportfeature.domain.repository.ReportRepository

class ReportRepositoryImpl(
    private val reportApiService: ReportApiService,
) : ReportRepository {
    override suspend fun getReportTypes(): Resource<List<String>> =
        reportApiService.getReportTypes()

    override suspend fun createPostReport(
        postId: Long,
        reportType: String,
        description: String?,
    ): Resource<CreatedReport> =
        reportApiService.createPostReport(
            postId = postId,
            reportType = reportType,
            description = description,
        )
}
