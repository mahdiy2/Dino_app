package com.dino.message.activitynotificationfeature.domain.usecase

import com.dino.message.activitynotificationfeature.domain.repository.ActivityNotificationRepository
import com.dino.message.corefeature.presentation.util.Resource
import kotlinx.coroutines.flow.flow

class GetUnreadCountUseCase constructor(
    private val repository: ActivityNotificationRepository
) {
    fun execute() = flow {
        emit(Resource.Loading())
        emit(repository.getUnreadCount())
    }
}
