package com.dino.message.groupfeature.domain.usecase

import com.dino.message.corefeature.presentation.util.Resource
import com.dino.message.groupfeature.domain.model.Group
import com.dino.message.groupfeature.domain.repository.GroupRepository
import kotlinx.coroutines.flow.flow

class GetGroupDetailUseCase constructor(
    private val repository: GroupRepository,
) {
    fun execute(groupId: Int) = flow<Resource<Group>> {
        emit(Resource.Loading())
        emit(repository.getGroupDetail(groupId))
    }
}
