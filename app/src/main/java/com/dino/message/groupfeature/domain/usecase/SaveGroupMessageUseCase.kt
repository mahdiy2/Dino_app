package com.dino.message.groupfeature.domain.usecase

import com.dino.message.groupfeature.domain.model.GroupMessageEntity
import com.dino.message.groupfeature.domain.repository.GroupRepository
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class SaveGroupMessageUseCase @Inject constructor(
    private val repository: GroupRepository
) {
    fun execute(message: GroupMessageEntity) = flow {
        emit(repository.saveGroupMessage(message))
    }
}
