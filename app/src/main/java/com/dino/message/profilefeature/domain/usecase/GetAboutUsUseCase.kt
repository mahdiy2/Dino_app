package com.dino.message.profilefeature.domain.usecase

import com.dino.message.corefeature.presentation.util.Resource
import com.dino.message.profilefeature.domain.model.AboutUs
import com.dino.message.profilefeature.domain.repository.ProfileRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.flow

class GetAboutUsUseCase @Inject constructor(
    private val profileRepository: ProfileRepository
) {
    fun execute() = flow<Resource<AboutUs>> {
        emit(Resource.Loading())
        emit(profileRepository.getAboutUs())
    }
}
