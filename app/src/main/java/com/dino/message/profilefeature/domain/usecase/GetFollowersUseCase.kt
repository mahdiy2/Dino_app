package com.dino.message.profilefeature.domain.usecase

import com.dino.message.corefeature.presentation.util.Resource
import com.dino.message.profilefeature.domain.model.ProfileUsersPage
import com.dino.message.profilefeature.domain.repository.ProfileRepository
import kotlinx.coroutines.flow.flow

class GetFollowersUseCase constructor(
    private val profileRepository: ProfileRepository
) {
    fun execute(username: String, page: Int, limit: Int) = flow<Resource<ProfileUsersPage>> {
        emit(Resource.Loading())
        emit(profileRepository.getFollowers(username = username, page = page, limit = limit))
    }
}
