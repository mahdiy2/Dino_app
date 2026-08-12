package com.dino.message.profilefeature.domain.usecase

import com.dino.message.corefeature.domain.result.Resource
import com.dino.message.profilefeature.domain.model.Profile
import com.dino.message.profilefeature.domain.repository.ProfileRepository
import kotlinx.coroutines.flow.flow

class GetMyProfileUseCase constructor(
    private val profileRepository: ProfileRepository
) {
    fun execute() = flow<Resource<Profile>> {
        emit(Resource.Loading())
        emit(profileRepository.getMyProfile())
    }
}
