package com.dino.message.reportfeature.domain.usecase

import com.dino.message.corefeature.presentation.util.Resource
import com.dino.message.reportfeature.domain.repository.ReportRepository
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class GetReportTypesUseCase @Inject constructor(
    private val reportRepository: ReportRepository,
) {
    fun execute() = flow<Resource<List<String>>> {
        emit(Resource.Loading())
        emit(reportRepository.getReportTypes())
    }
}
