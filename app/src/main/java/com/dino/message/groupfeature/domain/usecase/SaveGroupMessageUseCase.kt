package com.dino.message.groupfeature.domain.usecase

import com.dino.message.groupfeature.domain.model.GroupMessageEntity
import com.dino.message.groupfeature.domain.repository.GroupRepository
import kotlinx.coroutines.flow.flow

class SaveGroupMessageUseCase constructor(
    private val repository: GroupRepository
) {
    fun execute(message: GroupMessageEntity) = flow {
        emit(repository.saveGroupMessage(message))
    }
}
