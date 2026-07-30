package com.dino.message.profilefeature.domain.usecase

import com.dino.message.corefeature.presentation.util.Resource
import com.dino.message.profilefeature.domain.repository.ProfileRepository
import kotlinx.coroutines.flow.flow

class GetFollowStatusUseCase constructor(
    private val profileRepository: ProfileRepository
) {
    fun execute(username: String) = flow<Resource<Boolean>> {
        emit(Resource.Loading())
        emit(profileRepository.getFollowStatus(username = username))
    }
}
