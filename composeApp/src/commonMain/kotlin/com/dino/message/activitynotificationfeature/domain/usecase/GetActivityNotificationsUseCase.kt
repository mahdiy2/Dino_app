package com.dino.message.activitynotificationfeature.domain.usecase

import com.dino.message.activitynotificationfeature.domain.repository.ActivityNotificationRepository
import com.dino.message.corefeature.domain.result.Resource
import kotlinx.coroutines.flow.flow

class GetActivityNotificationsUseCase constructor(
    private val repository: ActivityNotificationRepository
) {
    fun execute(page: Int, limit: Int) = flow {
        emit(Resource.Loading())
        emit(repository.getNotifications(page = page, limit = limit))
    }
}
