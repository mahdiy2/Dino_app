package com.dino.message.profilefeature.domain.usecase

import com.dino.message.corefeature.presentation.util.Resource
import com.dino.message.profilefeature.domain.model.ProfileUsersPage
import com.dino.message.profilefeature.domain.repository.ProfileRepository
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class GetBlockedUsersUseCase @Inject constructor(
    private val profileRepository: ProfileRepository
) {
    fun execute(page: Int, limit: Int) = flow<Resource<ProfileUsersPage>> {
        emit(Resource.Loading())
        emit(profileRepository.getBlockedUsers(page = page, limit = limit))
    }
}
