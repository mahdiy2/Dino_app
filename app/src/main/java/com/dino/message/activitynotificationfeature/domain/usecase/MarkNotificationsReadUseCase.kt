package com.dino.message.activitynotificationfeature.domain.usecase

import com.dino.message.activitynotificationfeature.domain.repository.ActivityNotificationRepository
import com.dino.message.corefeature.presentation.util.Resource
import kotlinx.coroutines.flow.flow

class MarkNotificationsReadUseCase constructor(
    private val repository: ActivityNotificationRepository
) {
    fun execute(notificationIds: List<Long>? = null) = flow {
        emit(Resource.Loading())
        emit(repository.markRead(notificationIds = notificationIds))
    }
}
