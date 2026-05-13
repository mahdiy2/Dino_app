package com.dino.message.groupfeature.domain.usecase

import com.dino.message.corefeature.presentation.util.Resource
import com.dino.message.groupfeature.domain.model.PublicRoomStatus
import com.dino.message.groupfeature.domain.repository.GroupRepository
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class GetPublicRoomStatusUseCase @Inject constructor(
    private val repository: GroupRepository,
) {
    fun execute() = flow<Resource<PublicRoomStatus>> {
        emit(Resource.Loading())
        emit(repository.getPublicRoomStatus())
    }
}
