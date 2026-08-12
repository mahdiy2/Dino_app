package com.dino.message.groupfeature.domain.usecase

import com.dino.message.corefeature.domain.result.Resource
import com.dino.message.groupfeature.domain.model.PublicRoomStatus
import com.dino.message.groupfeature.domain.repository.GroupRepository
import kotlinx.coroutines.flow.flow

class GetPublicRoomStatusUseCase constructor(
    private val repository: GroupRepository,
) {
    fun execute() = flow<Resource<PublicRoomStatus>> {
        emit(Resource.Loading())
        emit(repository.getPublicRoomStatus())
    }
}
