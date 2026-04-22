package com.dino.message.groupfeature.domain.usecase

import com.dino.message.groupfeature.domain.repository.GroupRepository
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class GetGroupMessagesFromDbUseCase @Inject constructor(
    private val repository: GroupRepository
) {
    fun execute(groupId: Int, limit: Int, offset: Int = 0) = flow {
        emit(repository.getGroupMessagesFromDb(groupId = groupId, limit = limit, offset = offset))
    }
}
