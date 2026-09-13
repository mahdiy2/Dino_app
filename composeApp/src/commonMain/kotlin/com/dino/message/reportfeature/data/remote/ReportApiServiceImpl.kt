package com.dino.message.reportfeature.data.remote

import com.dino.message.corefeature.data.remote.HaveRefreshTokenAPI
import com.dino.message.corefeature.data.remote.HaveRefreshTokenAPIImpl
import com.dino.message.corefeature.data.remote.util.HttpAPIUtil
import com.dino.message.corefeature.data.remote.util.appendTokenToHeader
import com.dino.message.corefeature.domain.session.AuthSessionStore
import com.dino.message.corefeature.domain.result.Resource
import com.dino.message.reportfeature.data.remote.dto.CreatePostReportRequest
import com.dino.message.reportfeature.data.remote.dto.CreatePostReportResponse
import com.dino.message.reportfeature.data.remote.dto.GetReportTypesResponse
import com.dino.message.reportfeature.domain.model.CreatedReport
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

class ReportApiServiceImpl constructor(
    private val httpClient: HttpClient,
    private val sPrefManager: AuthSessionStore,
    private val httpAPIUtil: HttpAPIUtil,
) : ReportApiService,
    HaveRefreshTokenAPI by HaveRefreshTokenAPIImpl(httpClient, sPrefManager) {

    override suspend fun getReportTypes(): Resource<List<String>> =
        httpAPIUtil.callAPIWithErrorHandling<List<String>, GetReportTypesResponse>(
            callAPI = {
                httpClient.get(ReportRoute.GetReportTypes.url) {
                    contentType(ContentType.Application.Json)
                }
            },
            callRefreshTokenAPI = this::getToken,
            needToUpdateToken = false,
        )

    override suspend fun createPostReport(
        postId: Long,
        reportType: String,
        description: String?,
    ): Resource<CreatedReport> =
        httpAPIUtil.callAPIWithErrorHandling<CreatedReport, CreatePostReportResponse>(
            callAPI = {
                httpClient.post(ReportRoute.CreatePostReport(postId).url) {
                    contentType(ContentType.Application.Json)
                    appendTokenToHeader(sPrefManager.getToken())
                    setBody(
                        CreatePostReportRequest(
                            reportType = reportType,
                            description = description,
                        )
                    )
                }
            },
            callRefreshTokenAPI = this::getToken,
        )
}
