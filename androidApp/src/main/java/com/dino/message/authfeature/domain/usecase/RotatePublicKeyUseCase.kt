package com.dino.message.authfeature.domain.usecase

import com.dino.message.authfeature.domain.model.RotatePublicKeyUser
import com.dino.message.authfeature.domain.repository.AuthRepository
import com.dino.message.corefeature.domain.result.Resource
import kotlinx.coroutines.flow.flow

class RotatePublicKeyUseCase constructor(
    private val authRepository: AuthRepository,
) {
    fun execute(publicKey: String) = flow<Resource<RotatePublicKeyUser>> {
        emit(Resource.Loading())
        emit(authRepository.rotatePublicKey(publicKey))
    }
}
