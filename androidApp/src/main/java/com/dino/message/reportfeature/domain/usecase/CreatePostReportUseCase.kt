package com.dino.message.reportfeature.domain.usecase

import com.dino.message.corefeature.domain.result.Resource
import com.dino.message.reportfeature.domain.model.CreatedReport
import com.dino.message.reportfeature.domain.repository.ReportRepository
import kotlinx.coroutines.flow.flow

class CreatePostReportUseCase constructor(
    private val reportRepository: ReportRepository,
) {
    fun execute(
        postId: Long,
        reportType: String,
        description: String? = null,
    ) = flow<Resource<CreatedReport>> {
        emit(Resource.Loading())
        emit(
            reportRepository.createPostReport(
                postId = postId,
                reportType = reportType,
                description = description,
            )
        )
    }
}
