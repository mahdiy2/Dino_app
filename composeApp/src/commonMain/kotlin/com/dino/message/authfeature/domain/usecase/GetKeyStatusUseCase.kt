package com.dino.message.authfeature.domain.usecase

import com.dino.message.authfeature.domain.model.KeyStatusUser
import com.dino.message.authfeature.domain.repository.AuthRepository
import com.dino.message.corefeature.domain.result.Resource
import kotlinx.coroutines.flow.flow

class GetKeyStatusUseCase constructor(
    private val authRepository: AuthRepository,
) {
    fun execute() = flow<Resource<KeyStatusUser>> {
        emit(Resource.Loading())
        emit(authRepository.getKeyStatus())
    }
}
