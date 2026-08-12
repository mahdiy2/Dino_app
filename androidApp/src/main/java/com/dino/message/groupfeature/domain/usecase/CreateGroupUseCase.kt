package com.dino.message.groupfeature.domain.usecase

import com.dino.message.corefeature.domain.result.Resource
import com.dino.message.groupfeature.domain.model.Group
import com.dino.message.groupfeature.domain.repository.GroupRepository
import kotlinx.coroutines.flow.flow

class CreateGroupUseCase constructor(
    private val repository: GroupRepository,
) {
    fun execute(name: String, members: List<String>) = flow<Resource<Group>> {
        emit(Resource.Loading())
        emit(repository.createGroup(name, members))
    }
}
