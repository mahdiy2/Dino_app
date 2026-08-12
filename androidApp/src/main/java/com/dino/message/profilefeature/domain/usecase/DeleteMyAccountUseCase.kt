package com.dino.message.profilefeature.domain.usecase

import com.dino.message.corefeature.presentation.util.Resource
import com.dino.message.profilefeature.domain.model.ProfileFollowResult
import com.dino.message.profilefeature.domain.repository.ProfileRepository
import kotlinx.coroutines.flow.flow

class DeleteMyAccountUseCase constructor(
    private val profileRepository: ProfileRepository
) {
    fun execute() = flow<Resource<ProfileFollowResult>> {
        emit(Resource.Loading())
        emit(profileRepository.deleteMyAccount())
    }
}
