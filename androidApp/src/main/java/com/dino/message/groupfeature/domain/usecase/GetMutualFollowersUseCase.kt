package com.dino.message.groupfeature.domain.usecase

import com.dino.message.corefeature.domain.result.Resource
import com.dino.message.groupfeature.domain.model.MutualFollowersPage
import com.dino.message.groupfeature.domain.repository.GroupRepository
import kotlinx.coroutines.flow.flow

class GetMutualFollowersUseCase constructor(
    private val repository: GroupRepository,
) {
    fun execute(page: Int = 1, limit: Int = 50) = flow<Resource<MutualFollowersPage>> {
        emit(Resource.Loading())
        emit(repository.getMutualFollowers(page, limit))
    }
}
