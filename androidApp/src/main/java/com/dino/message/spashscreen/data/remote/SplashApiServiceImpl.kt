package com.dino.message.spashscreen.data.remote

import com.dino.message.corefeature.data.remote.util.HttpAPIUtil
import com.dino.message.corefeature.domain.result.Resource
import com.dino.message.spashscreen.data.remote.dto.CheckAppVersionRequest
import com.dino.message.spashscreen.data.remote.dto.CheckAppVersionResponse
import com.dino.message.spashscreen.domain.model.VersionCheckDecision
import com.dino.message.spashscreen.domain.repository.SplashApiService
import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

class SplashApiServiceImpl constructor(
    private val httpClient: HttpClient,
    private val httpAPIUtil: HttpAPIUtil,
) : SplashApiService {

    override suspend fun checkAppVersion(version: String): Resource<VersionCheckDecision> {
        return httpAPIUtil.callAPIWithErrorHandling<VersionCheckDecision, CheckAppVersionResponse>(
            callAPI = {
                httpClient.post(SplashRoute.CheckAppVersion.url) {
                    contentType(ContentType.Application.Json)
                    setBody(
                        CheckAppVersionRequest(
                            platform = "android",
                            version = version,
                        )
                    )
                }
            },
            callRefreshTokenAPI = { "" },
            needToUpdateToken = false,
        )
    }
}
