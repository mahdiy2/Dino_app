package com.dino.message.groupfeature.domain.usecase

import com.dino.message.corefeature.presentation.util.Resource
import com.dino.message.groupfeature.domain.model.Group
import com.dino.message.groupfeature.domain.repository.GroupRepository
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class ListGroupsUseCase @Inject constructor(
    private val repository: GroupRepository,
) {
    fun execute() = flow<Resource<List<Group>>> {
        emit(Resource.Loading())
        emit(repository.listGroups())
    }
}
