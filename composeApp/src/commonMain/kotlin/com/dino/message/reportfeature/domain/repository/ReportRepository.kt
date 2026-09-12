package com.dino.message.reportfeature.domain.repository

import com.dino.message.corefeature.domain.result.Resource
import com.dino.message.reportfeature.domain.model.CreatedReport

interface ReportRepository {
    suspend fun getReportTypes(): Resource<List<String>>

    suspend fun createPostReport(
        postId: Long,
        reportType: String,
        description: String? = null,
    ): Resource<CreatedReport>
}
