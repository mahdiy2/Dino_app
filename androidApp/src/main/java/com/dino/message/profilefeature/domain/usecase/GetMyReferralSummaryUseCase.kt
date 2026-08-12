package com.dino.message.profilefeature.domain.usecase

import com.dino.message.corefeature.presentation.util.Resource
import com.dino.message.profilefeature.domain.model.ReferralSummary
import com.dino.message.profilefeature.domain.repository.ProfileRepository
import kotlinx.coroutines.flow.flow

class GetMyReferralSummaryUseCase constructor(
    private val profileRepository: ProfileRepository
) {
    fun execute() = flow<Resource<ReferralSummary>> {
        emit(Resource.Loading())
        emit(profileRepository.getMyReferralSummary())
    }
}
