package com.dino.message.authfeature.domain.usecase

import com.dino.message.authfeature.domain.model.RegisterStartUser
import com.dino.message.authfeature.domain.repository.AuthRepository
import com.dino.message.corefeature.presentation.util.Resource
import kotlinx.coroutines.flow.flow

class RegisterUseCase constructor(
    private val authRepository: AuthRepository
) {
    fun execute(
        username: String,
        pass: String,
        publicKey: String,
        clientNonce: String,
        referralCode: String?,
        deviceId: String?
    ) = flow<Resource<RegisterStartUser>> {
        emit(Resource.Loading())
        emit(
            authRepository.registerStart(
                userName = username,
                pass = pass,
                publicKey = publicKey,
                clientNonce = clientNonce,
                referralCode = referralCode,
                deviceId = deviceId
            )
        )
    }
}
