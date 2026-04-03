package com.dino.message.groupfeature.domain.usecase

import com.dino.message.groupfeature.domain.repository.GroupRepository
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class GetGroupMessagesFromDbUseCase @Inject constructor(
    private val repository: GroupRepository
) {
    fun execute(groupId: Int) = flow {
        emit(repository.getGroupMessagesFromDb(groupId))
    }
}
