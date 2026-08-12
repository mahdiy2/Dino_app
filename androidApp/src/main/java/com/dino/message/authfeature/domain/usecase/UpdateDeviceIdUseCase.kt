package com.dino.message.authfeature.domain.usecase

import com.dino.message.authfeature.domain.repository.AuthRepository
import com.dino.message.corefeature.domain.result.Resource
import com.dino.message.profilefeature.domain.model.ProfileFollowResult
import kotlinx.coroutines.flow.flow

class UpdateDeviceIdUseCase constructor(
    private val authRepository: AuthRepository
) {
    fun execute(deviceId: String) = flow<Resource<ProfileFollowResult>> {
        emit(Resource.Loading())
        emit(authRepository.updateDeviceId(deviceId = deviceId))
    }
}
