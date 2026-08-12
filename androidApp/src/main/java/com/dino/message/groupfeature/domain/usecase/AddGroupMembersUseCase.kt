package com.dino.message.groupfeature.domain.usecase

import com.dino.message.corefeature.domain.result.Resource
import com.dino.message.groupfeature.domain.model.AddGroupMembersResult
import com.dino.message.groupfeature.domain.repository.GroupRepository
import kotlinx.coroutines.flow.flow

class AddGroupMembersUseCase constructor(
    private val repository: GroupRepository,
) {
    fun execute(groupId: Int, usernames: List<String>) = flow<Resource<AddGroupMembersResult>> {
        emit(Resource.Loading())
        emit(repository.addGroupMembers(groupId, usernames))
    }
}
