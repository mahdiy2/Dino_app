package com.dino.message.authfeature.domain.usecase

import com.dino.message.authfeature.domain.model.RegisterConfirmUser
import com.dino.message.authfeature.domain.repository.AuthRepository
import com.dino.message.corefeature.presentation.util.Resource
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class ConfirmRegistrationUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    fun execute(registrationId: String) = flow<Resource<RegisterConfirmUser>> {
        emit(Resource.Loading())
        emit(authRepository.registerConfirm(registrationId))
    }
}
