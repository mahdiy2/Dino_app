package com.dino.message.activitynotificationfeature.data.remote

import com.dino.message.activitynotificationfeature.data.remote.dto.GetActivityNotificationsResponse
import com.dino.message.activitynotificationfeature.data.remote.dto.MarkReadRequest
import com.dino.message.activitynotificationfeature.data.remote.dto.MarkReadResponse
import com.dino.message.activitynotificationfeature.data.remote.dto.UnreadCountResponse
import com.dino.message.activitynotificationfeature.domain.model.ActivityNotificationsPage
import com.dino.message.activitynotificationfeature.domain.model.MarkReadResult
import com.dino.message.activitynotificationfeature.domain.model.UnreadCount
import com.dino.message.corefeature.data.remote.HaveRefreshTokenAPI
import com.dino.message.corefeature.data.remote.HaveRefreshTokenAPIImpl
import com.dino.message.corefeature.data.remote.util.HttpAPIUtil
import com.dino.message.corefeature.data.remote.util.appendTokenToHeader
import com.dino.message.corefeature.data.spref.SPrefManager
import com.dino.message.corefeature.presentation.util.Resource
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import javax.inject.Inject

class ActivityNotificationApiServiceImpl @Inject constructor(
    private val httpClient: HttpClient,
    private val sPrefManager: SPrefManager,
    private val httpAPIUtil: HttpAPIUtil
) : ActivityNotificationApiService,
    HaveRefreshTokenAPI by HaveRefreshTokenAPIImpl(httpClient, sPrefManager) {

    override suspend fun getNotifications(
        page: Int,
        limit: Int
    ): Resource<ActivityNotificationsPage> =
        httpAPIUtil.callAPIWithErrorHandling<ActivityNotificationsPage, GetActivityNotificationsResponse>(
            callAPI = {
                httpClient.get(
                    ActivityNotificationRoute.GetNotifications(page = page, limit = limit).url
                ) {
                    contentType(ContentType.Application.Json)
                    appendTokenToHeader(sPrefManager.getToken())
                }
            },
            callRefreshTokenAPI = this::getToken
        )

    override suspend fun getUnreadCount(): Resource<UnreadCount> =
        httpAPIUtil.callAPIWithErrorHandling<UnreadCount, UnreadCountResponse>(
            callAPI = {
                httpClient.get(ActivityNotificationRoute.UnreadCount.url) {
                    contentType(ContentType.Application.Json)
                    appendTokenToHeader(sPrefManager.getToken())
                }
            },
            callRefreshTokenAPI = this::getToken
        )

    override suspend fun markRead(notificationIds: List<Long>?): Resource<MarkReadResult> =
        httpAPIUtil.callAPIWithErrorHandling<MarkReadResult, MarkReadResponse>(
            callAPI = {
                httpClient.post(ActivityNotificationRoute.MarkRead.url) {
                    contentType(ContentType.Application.Json)
                    appendTokenToHeader(sPrefManager.getToken())
                    setBody(MarkReadRequest(notification_ids = notificationIds))
                }
            },
            callRefreshTokenAPI = this::getToken
        )
}
