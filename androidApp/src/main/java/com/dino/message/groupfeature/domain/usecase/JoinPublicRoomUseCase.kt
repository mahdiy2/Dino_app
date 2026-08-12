package com.dino.message.groupfeature.domain.usecase

import com.dino.message.corefeature.presentation.util.Resource
import com.dino.message.groupfeature.domain.model.JoinPublicRoomResult
import com.dino.message.groupfeature.domain.repository.GroupRepository
import kotlinx.coroutines.flow.flow

class JoinPublicRoomUseCase constructor(
    private val repository: GroupRepository,
) {
    fun execute() = flow<Resource<JoinPublicRoomResult>> {
        emit(Resource.Loading())
        emit(repository.joinPublicRoom())
    }
}
