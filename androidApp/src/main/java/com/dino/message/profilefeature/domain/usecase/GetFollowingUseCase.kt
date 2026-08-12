package com.dino.message.profilefeature.domain.usecase

import com.dino.message.corefeature.domain.result.Resource
import com.dino.message.profilefeature.domain.model.ProfileUsersPage
import com.dino.message.profilefeature.domain.repository.ProfileRepository
import kotlinx.coroutines.flow.flow

class GetFollowingUseCase constructor(
    private val profileRepository: ProfileRepository
) {
    fun execute(username: String, page: Int, limit: Int) = flow<Resource<ProfileUsersPage>> {
        emit(Resource.Loading())
        emit(profileRepository.getFollowing(username = username, page = page, limit = limit))
    }
}
