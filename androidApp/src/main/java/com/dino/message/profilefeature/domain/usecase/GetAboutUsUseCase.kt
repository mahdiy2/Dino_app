package com.dino.message.profilefeature.domain.usecase

import com.dino.message.corefeature.domain.result.Resource
import com.dino.message.profilefeature.domain.model.AboutUs
import com.dino.message.profilefeature.domain.repository.ProfileRepository
import kotlinx.coroutines.flow.flow

class GetAboutUsUseCase constructor(
    private val profileRepository: ProfileRepository
) {
    fun execute() = flow<Resource<AboutUs>> {
        emit(Resource.Loading())
        emit(profileRepository.getAboutUs())
    }
}
