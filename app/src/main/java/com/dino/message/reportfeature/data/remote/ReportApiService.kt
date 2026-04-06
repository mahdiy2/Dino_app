package com.dino.message.reportfeature.data.remote

import com.dino.message.corefeature.presentation.util.Resource
import com.dino.message.reportfeature.domain.model.CreatedReport

interface ReportApiService {
    suspend fun getReportTypes(): Resource<List<String>>

    suspend fun createPostReport(
        postId: Long,
        reportType: String,
        description: String? = null,
    ): Resource<CreatedReport>
}
