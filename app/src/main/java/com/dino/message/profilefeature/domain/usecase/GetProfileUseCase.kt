package com.dino.message.profilefeature.domain.usecase

import com.dino.message.corefeature.presentation.util.Resource
import com.dino.message.profilefeature.domain.model.Profile
import com.dino.message.profilefeature.domain.repository.ProfileRepository
import kotlinx.coroutines.flow.flow

class GetProfileUseCase constructor(
    private val profileRepository: ProfileRepository
) {
    fun execute(username: String) = flow<Resource<Profile>> {
        emit(Resource.Loading())
        emit(profileRepository.getProfile(username = username))
    }
}
