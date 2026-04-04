package com.dino.message.authfeature.data.remote

import com.dino.message.authfeature.domain.model.LoginUser
import com.dino.message.authfeature.domain.model.RegisterConfirmUser
import com.dino.message.authfeature.domain.model.RegisterStartUser
import com.dino.message.authfeature.domain.model.RegisterUser
import com.dino.message.authfeature.domain.model.RotatePublicKeyUser
import com.dino.message.corefeature.presentation.util.Resource

interface AuthAPIService {

    suspend fun register(
        userName: String,
        pass: String,
        publicKey: String
    ): Resource<RegisterUser>

    suspend fun registerStart(
        userName: String,
        pass: String,
        publicKey: String,
        clientNonce: String,
    ): Resource<RegisterStartUser>

    suspend fun registerConfirm(
        registrationId: String
    ): Resource<RegisterConfirmUser>

    suspend fun login(
        userName: String,
        pass: String,
    ): Resource<LoginUser>

    suspend fun rotatePublicKey(
        publicKey: String,
    ): Resource<RotatePublicKeyUser>
}
