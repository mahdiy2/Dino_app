package com.dino.message.authfeature.data.repository

import com.dino.message.authfeature.data.remote.AuthAPIService
import com.dino.message.authfeature.domain.repository.AuthRepository

class AuthRepositoryImpl(
    private val authAPIService: AuthAPIService

): AuthRepository {
    override suspend fun register(
        userName: String,
        pass: String,
        publicKey: String
    ) = authAPIService.register(userName, pass, publicKey)

    override suspend fun registerStart(
        userName: String,
        pass: String,
        publicKey: String,
        clientNonce: String
    ) = authAPIService.registerStart(userName, pass, publicKey, clientNonce)

    override suspend fun registerConfirm(
        registrationId: String
    ) = authAPIService.registerConfirm(registrationId)

    override suspend fun login(
        userName: String,
        pass: String
    ) = authAPIService.login(userName, pass)

    override suspend fun getKeyStatus() = authAPIService.getKeyStatus()

    override suspend fun rotatePublicKey(
        publicKey: String
    ) = authAPIService.rotatePublicKey(publicKey)

}
