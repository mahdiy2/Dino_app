package com.dino.message.activitynotificationfeature.domain.usecase

import com.dino.message.activitynotificationfeature.domain.repository.ActivityNotificationRepository
import com.dino.message.corefeature.presentation.util.Resource
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class GetActivityNotificationsUseCase @Inject constructor(
    private val repository: ActivityNotificationRepository
) {
    fun execute(page: Int, limit: Int) = flow {
        emit(Resource.Loading())
        emit(repository.getNotifications(page = page, limit = limit))
    }
}
