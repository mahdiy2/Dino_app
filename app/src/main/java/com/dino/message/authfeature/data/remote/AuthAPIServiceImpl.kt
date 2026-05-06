package com.dino.message.authfeature.data.remote

import com.dino.message.authfeature.data.remote.dto.LoginRequest
import com.dino.message.authfeature.data.remote.dto.LoginResponse
import com.dino.message.authfeature.data.remote.dto.KeyStatusResponse
import com.dino.message.authfeature.data.remote.dto.RegisterConfirmRequest
import com.dino.message.authfeature.data.remote.dto.RegisterConfirmResponse
import com.dino.message.authfeature.data.remote.dto.RegisterRequest
import com.dino.message.authfeature.data.remote.dto.RotatePublicKeyRequest
import com.dino.message.authfeature.data.remote.dto.RotatePublicKeyResponse
import com.dino.message.authfeature.data.remote.dto.RegisterStartRequest
import com.dino.message.authfeature.data.remote.dto.RegisterStartResponse
import com.dino.message.authfeature.data.remote.dto.RegisterResponse
import com.dino.message.authfeature.data.remote.dto.UpdateDeviceIdRequest
import com.dino.message.authfeature.data.remote.dto.UpdateDeviceIdResponse
import com.dino.message.authfeature.domain.model.LoginUser
import com.dino.message.authfeature.domain.model.KeyStatusUser
import com.dino.message.authfeature.domain.model.RegisterConfirmUser
import com.dino.message.authfeature.domain.model.RegisterStartUser
import com.dino.message.authfeature.domain.model.RegisterUser
import com.dino.message.authfeature.domain.model.RotatePublicKeyUser
import com.dino.message.corefeature.data.remote.HaveRefreshTokenAPI
import com.dino.message.corefeature.data.remote.HaveRefreshTokenAPIImpl
import com.dino.message.corefeature.data.remote.util.HttpAPIUtil
import com.dino.message.corefeature.data.remote.util.appendTokenToHeader
import com.dino.message.corefeature.data.spref.SPrefManager
import com.dino.message.corefeature.presentation.util.Resource
import com.dino.message.profilefeature.domain.model.ProfileFollowResult
import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.client.request.get
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import javax.inject.Inject

class AuthAPIServiceImpl @Inject constructor(
    private val httpClient: HttpClient,
    private val sPrefManager: SPrefManager,
    private val httpAPIUtil: HttpAPIUtil): AuthAPIService, HaveRefreshTokenAPI by HaveRefreshTokenAPIImpl(httpClient, sPrefManager) {


    override suspend fun register(
        userName: String,
        pass: String,
        publicKey: String,
        referralCode: String?,
        deviceId: String?
    ) = httpAPIUtil.callAPIWithErrorHandling<RegisterUser, RegisterResponse>(
        callAPI = {
            httpClient.post(AuthRoute.RegisterLegacy.url) {
                contentType(ContentType.Application.Json)
                setBody(
                    RegisterRequest(
                        username = userName,
                        password = pass,
                        public_key = publicKey,
                        referral_code = referralCode,
                        device_id = deviceId
                    )
                )
            }
        },
        callRefreshTokenAPI = this::getToken,
        needToUpdateToken = false
    )

    override suspend fun registerStart(
        userName: String,
        pass: String,
        publicKey: String,
        clientNonce: String,
        referralCode: String?,
        deviceId: String?,
    ) = httpAPIUtil.callAPIWithErrorHandling<RegisterStartUser, RegisterStartResponse>(
        callAPI = {
            httpClient.post(AuthRoute.RegisterStart.url) {
                contentType(ContentType.Application.Json)
                setBody(
                    RegisterStartRequest(
                        username = userName,
                        password = pass,
                        public_key = publicKey,
                        client_nonce = clientNonce,
                        referral_code = referralCode,
                        device_id = deviceId,
                    )
                )
            }
        },
        callRefreshTokenAPI = this::getToken,
        needToUpdateToken = false
    )

    override suspend fun registerConfirm(
        registrationId: String
    ) = httpAPIUtil.callAPIWithErrorHandling<RegisterConfirmUser, RegisterConfirmResponse>(
        callAPI = {
            httpClient.post(AuthRoute.RegisterConfirm.url) {
                contentType(ContentType.Application.Json)
                setBody(
                    RegisterConfirmRequest(
                        registration_id = registrationId
                    )
                )
            }
        },
        callRefreshTokenAPI = this::getToken,
        needToUpdateToken = false
    )

    override suspend fun login(
        userName: String,
        pass: String
    ): Resource<LoginUser> =
        httpAPIUtil.callAPIWithErrorHandling<LoginUser, LoginResponse>(
            callAPI = {
                httpClient.post(AuthRoute.Login.url) {
                    contentType(ContentType.Application.Json)
                    setBody(
                        LoginRequest(
                            username = userName,
                            password = pass,
                        )
                    )
                }
            },
            callRefreshTokenAPI = this::getToken,
            needToUpdateToken = false,
            useServerMessageForUnauthorized = true
        )

    override suspend fun rotatePublicKey(
        publicKey: String
    ): Resource<RotatePublicKeyUser> =
        httpAPIUtil.callAPIWithErrorHandling<RotatePublicKeyUser, RotatePublicKeyResponse>(
            callAPI = {
                httpClient.post(AuthRoute.RotateKey.url) {
                    contentType(ContentType.Application.Json)
                    appendTokenToHeader(sPrefManager.getToken())
                    setBody(
                        RotatePublicKeyRequest(
                            public_key = publicKey
                        )
                    )
                }
            },
            callRefreshTokenAPI = this::getToken
        )

    override suspend fun getKeyStatus(): Resource<KeyStatusUser> =
        httpAPIUtil.callAPIWithErrorHandling<KeyStatusUser, KeyStatusResponse>(
            callAPI = {
                httpClient.get(AuthRoute.KeyStatus.url) {
                    contentType(ContentType.Application.Json)
                    appendTokenToHeader(sPrefManager.getToken())
                }
            },
            callRefreshTokenAPI = this::getToken
        )

    override suspend fun updateDeviceId(
        deviceId: String
    ): Resource<ProfileFollowResult> =
        httpAPIUtil.callAPIWithErrorHandling<ProfileFollowResult, UpdateDeviceIdResponse>(
            callAPI = {
                httpClient.post(AuthRoute.UpdateDeviceId.url) {
                    contentType(ContentType.Application.Json)
                    appendTokenToHeader(sPrefManager.getToken())
                    setBody(
                        UpdateDeviceIdRequest(
                            device_id = deviceId
                        )
                    )
                }
            },
            callRefreshTokenAPI = this::getToken
        )
}
