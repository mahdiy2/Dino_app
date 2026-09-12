package com.dino.message.authfeature.domain.usecase

import com.dino.message.authfeature.domain.model.RegisterConfirmUser
import com.dino.message.authfeature.domain.repository.AuthRepository
import com.dino.message.corefeature.domain.result.Resource
import kotlinx.coroutines.flow.flow

class ConfirmRegistrationUseCase constructor(
    private val authRepository: AuthRepository
) {
    fun execute(registrationId: String) = flow<Resource<RegisterConfirmUser>> {
        emit(Resource.Loading())
        emit(authRepository.registerConfirm(registrationId))
    }
}
