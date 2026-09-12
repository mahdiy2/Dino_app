package com.dino.message.reportfeature.domain.usecase

import com.dino.message.corefeature.domain.result.Resource
import com.dino.message.reportfeature.domain.repository.ReportRepository
import kotlinx.coroutines.flow.flow

class GetReportTypesUseCase constructor(
    private val reportRepository: ReportRepository,
) {
    fun execute() = flow<Resource<List<String>>> {
        emit(Resource.Loading())
        emit(reportRepository.getReportTypes())
    }
}
