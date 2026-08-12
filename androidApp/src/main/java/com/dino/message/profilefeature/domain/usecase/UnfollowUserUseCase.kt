package com.dino.message.profilefeature.domain.usecase

import com.dino.message.corefeature.domain.result.Resource
import com.dino.message.profilefeature.domain.model.ProfileFollowResult
import com.dino.message.profilefeature.domain.repository.ProfileRepository
import kotlinx.coroutines.flow.flow

class UnfollowUserUseCase constructor(
    private val profileRepository: ProfileRepository
) {
    fun execute(username: String) = flow<Resource<ProfileFollowResult>> {
        emit(Resource.Loading())
        emit(profileRepository.unfollowUser(username = username))
    }
}
