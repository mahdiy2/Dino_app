package com.dino.message.groupfeature.data.remote

import android.content.Context
import com.dino.message.chatfeature.data.remote.dto.MessageAttachment
import com.dino.message.corefeature.data.remote.HaveRefreshTokenAPI
import com.dino.message.corefeature.data.remote.HaveRefreshTokenAPIImpl
import com.dino.message.corefeature.data.remote.logging.AppLogger
import com.dino.message.corefeature.data.remote.util.HttpAPIUtil
import com.dino.message.corefeature.data.remote.util.appendTokenToHeader
import com.dino.message.corefeature.data.remote.util.ensureFreshAccessTokenIfNearExpiry
import com.dino.message.corefeature.data.spref.SPrefManager
import com.dino.message.corefeature.data.upload.AndroidUploadContentProvider
import com.dino.message.corefeature.domain.upload.AttachmentUploadPolicy
import com.dino.message.corefeature.data.upload.ReadableMediaFile
import com.dino.message.corefeature.data.upload.appendUploadFile
import com.dino.message.corefeature.data.upload.toUploadFilePart
import com.dino.message.corefeature.domain.model.asStringResourceContent
import com.dino.message.corefeature.domain.system.AppClock
import com.dino.message.corefeature.presentation.util.Resource
import com.dino.message.groupfeature.data.remote.dto.AddGroupMembersRequest
import com.dino.message.groupfeature.data.remote.dto.AddGroupMembersResponse
import com.dino.message.groupfeature.data.remote.dto.CreateGroupRequest
import com.dino.message.groupfeature.data.remote.dto.CreateGroupResponse
import com.dino.message.groupfeature.data.remote.dto.GroupDetailResponse
import com.dino.message.groupfeature.data.remote.dto.GroupHistoryResponse
import com.dino.message.groupfeature.data.remote.dto.GroupOnlineUsersResponse
import com.dino.message.groupfeature.data.remote.dto.GroupUploadAttachmentResponse
import com.dino.message.groupfeature.data.remote.dto.LeaveGroupResponse
import com.dino.message.groupfeature.data.remote.dto.ListGroupsResponse
import com.dino.message.groupfeature.data.remote.dto.MutualFollowersResponse
import com.dino.message.groupfeature.data.remote.dto.PublicRoomStatusResponse
import com.dino.message.groupfeature.data.remote.dto.JoinPublicRoomResponse
import com.dino.message.chatfeature.data.remote.dto.ReactToMessageRequest
import com.dino.message.chatfeature.data.remote.dto.ReactToMessageResponse
import com.dino.message.groupfeature.domain.model.AddGroupMembersResult
import com.dino.message.groupfeature.domain.model.Group
import com.dino.message.groupfeature.domain.model.GroupHistoryPage
import com.dino.message.groupfeature.domain.model.GroupOnlineUser
import com.dino.message.groupfeature.domain.model.JoinPublicRoomResult
import com.dino.message.groupfeature.domain.model.MutualFollowersPage
import com.dino.message.groupfeature.domain.model.PublicRoomStatus
import io.ktor.client.HttpClient
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.plugins.timeout
import io.ktor.http.ContentType
import io.ktor.http.contentType

class GroupApiServiceImpl constructor(
    private val httpClient: HttpClient,
    private val sPrefManager: SPrefManager,
    private val httpAPIUtil: HttpAPIUtil,
    private val uploadContentProvider: AndroidUploadContentProvider,
    private val appContext: Context,
    private val appClock: AppClock,
    private val appLogger: AppLogger,
) : GroupApiService, HaveRefreshTokenAPI by HaveRefreshTokenAPIImpl(httpClient, sPrefManager) {
    companion object {
        private const val TAG = "GroupApiServiceImpl"
        private const val ATTACHMENT_UPLOAD_TIMEOUT_MS = 120_000L
    }

    override suspend fun createGroup(name: String, members: List<String>): Resource<Group> =
        httpAPIUtil.callAPIWithErrorHandling<Group, CreateGroupResponse>(
            callAPI = {
                httpClient.post(GroupRoute.CreateGroup.url) {
                    contentType(ContentType.Application.Json)
                    appendTokenToHeader(sPrefManager.getToken())
                    setBody(CreateGroupRequest(name = name, members = members))
                }
            },
            callRefreshTokenAPI = this::getToken
        )

    override suspend fun addGroupMembers(
        groupId: Int,
        usernames: List<String>,
    ): Resource<AddGroupMembersResult> =
        httpAPIUtil.callAPIWithErrorHandling<AddGroupMembersResult, AddGroupMembersResponse>(
            callAPI = {
                httpClient.post(GroupRoute.GroupMembers(groupId).url) {
                    contentType(ContentType.Application.Json)
                    appendTokenToHeader(sPrefManager.getToken())
                    setBody(AddGroupMembersRequest(usernames = usernames))
                }
            },
            callRefreshTokenAPI = this::getToken
        )

    override suspend fun listGroups(): Resource<List<Group>> =
        httpAPIUtil.callAPIWithErrorHandling<List<Group>, ListGroupsResponse>(
            callAPI = {
                httpClient.get(GroupRoute.ListGroups.url) {
                    contentType(ContentType.Application.Json)
                    appendTokenToHeader(sPrefManager.getToken())
                }
            },
            callRefreshTokenAPI = this::getToken
        )

    override suspend fun getGroupDetail(groupId: Int): Resource<Group> =
        httpAPIUtil.callAPIWithErrorHandling<Group, GroupDetailResponse>(
            callAPI = {
                httpClient.get(GroupRoute.GroupDetail(groupId).url) {
                    contentType(ContentType.Application.Json)
                    appendTokenToHeader(sPrefManager.getToken())
                }
            },
            callRefreshTokenAPI = this::getToken
        )

    override suspend fun leaveGroup(groupId: Int): Resource<Unit> =
        httpAPIUtil.callAPIWithErrorHandling<Unit, LeaveGroupResponse>(
            callAPI = {
                httpClient.post(GroupRoute.LeaveGroup(groupId).url) {
                    contentType(ContentType.Application.Json)
                    appendTokenToHeader(sPrefManager.getToken())
                }
            },
            callRefreshTokenAPI = this::getToken
        )

    override suspend fun getGroupOnlineUsers(groupId: Int): Resource<List<GroupOnlineUser>> =
        httpAPIUtil.callAPIWithErrorHandling<List<GroupOnlineUser>, GroupOnlineUsersResponse>(
            callAPI = {
                httpClient.get(GroupRoute.GroupOnlineUsers(groupId).url) {
                    contentType(ContentType.Application.Json)
                    appendTokenToHeader(sPrefManager.getToken())
                }
            },
            callRefreshTokenAPI = this::getToken
        )

    override suspend fun getGroupHistory(
        groupId: Int,
        limit: Int,
        before: String?,
    ): Resource<GroupHistoryPage> =
        httpAPIUtil.callAPIWithErrorHandling<GroupHistoryPage, GroupHistoryResponse>(
            callAPI = {
                httpClient.get(GroupRoute.GroupHistory(groupId).url) {
                    contentType(ContentType.Application.Json)
                    appendTokenToHeader(sPrefManager.getToken())
                    parameter("limit", limit)
                    if (!before.isNullOrBlank()) {
                        parameter("before", before)
                    }
                }
            },
            callRefreshTokenAPI = this::getToken
        )

    override suspend fun getMutualFollowers(page: Int, limit: Int): Resource<MutualFollowersPage> =
        httpAPIUtil.callAPIWithErrorHandling<MutualFollowersPage, MutualFollowersResponse>(
            callAPI = {
                httpClient.get(GroupRoute.MutualFollowers.url) {
                    contentType(ContentType.Application.Json)
                    appendTokenToHeader(sPrefManager.getToken())
                    parameter("page", page)
                    parameter("limit", limit)
                }
            },
            callRefreshTokenAPI = this::getToken
        )

    override suspend fun getPublicRoomStatus(): Resource<PublicRoomStatus> =
        httpAPIUtil.callAPIWithErrorHandling<PublicRoomStatus, PublicRoomStatusResponse>(
            callAPI = {
                httpClient.get(GroupRoute.PublicRoomStatus.url) {
                    contentType(ContentType.Application.Json)
                    appendTokenToHeader(sPrefManager.getToken())
                }
            },
            callRefreshTokenAPI = this::getToken
        )

    override suspend fun joinPublicRoom(): Resource<JoinPublicRoomResult> =
        httpAPIUtil.callAPIWithErrorHandling<JoinPublicRoomResult, JoinPublicRoomResponse>(
            callAPI = {
                httpClient.post(GroupRoute.JoinPublicRoom.url) {
                    contentType(ContentType.Application.Json)
                    appendTokenToHeader(sPrefManager.getToken())
                }
            },
            callRefreshTokenAPI = this::getToken
        )

    override suspend fun uploadGroupAttachment(groupId: Int, file: ReadableMediaFile): Resource<MessageAttachment> {
        val mimeType = AttachmentUploadPolicy.normalizeMimeType(file.handle.mimeType)
        val fileName = file.handle.displayName
        val fileSize = file.handle.sizeBytes
        val validationError = AttachmentUploadPolicy.validate(
            mimeType = mimeType,
            fileSizeBytes = fileSize,
        )
        if (validationError != null) {
            return Resource.Error(message = validationError.asStringResourceContent())
        }
        if (!uploadContentProvider.canRead(file)) {
            return Resource.Error(message = "Cannot read file".asStringResourceContent())
        }
        val startedAt = appClock.currentTimeMillis()
        appLogger.debug(
            TAG,
            "upload_start scope=group groupId=$groupId mime=$mimeType sizeBytes=${fileSize ?: -1} name=$fileName"
        )
        ensureFreshAccessTokenIfNearExpiry(
            sPrefManager = sPrefManager,
            refreshAccessToken = this::getToken,
            logTag = TAG
        )

        val result = httpAPIUtil.callAPIWithErrorHandling<MessageAttachment, GroupUploadAttachmentResponse>(
            callAPI = {
                httpClient.post(GroupRoute.GroupAttachments(groupId).url) {
                    appendTokenToHeader(sPrefManager.getToken())
                    timeout {
                        requestTimeoutMillis = ATTACHMENT_UPLOAD_TIMEOUT_MS
                        socketTimeoutMillis = ATTACHMENT_UPLOAD_TIMEOUT_MS
                    }
                    setBody(
                        MultiPartFormDataContent(
                            formData {
                                appendUploadFile(
                                    file.copy(
                                        handle = file.handle.copy(mimeType = mimeType)
                                    ).toUploadFilePart(formName = "file")
                                )
                            }
                        )
                    )
                }
            },
            callRefreshTokenAPI = this::getToken
        )
        val elapsedMs = appClock.currentTimeMillis() - startedAt
        return when (result) {
            is Resource.Success -> {
                appLogger.debug(
                    TAG,
                    "upload_success scope=group groupId=$groupId mime=$mimeType sizeBytes=${fileSize ?: -1} durationMs=$elapsedMs"
                )
                result
            }
            is Resource.Error -> {
                val errorMessage = result.message.asString(appContext)
                val timeoutLike = isLikelyWeakNetworkError(errorMessage)
                appLogger.warning(
                    TAG,
                    "upload_failed scope=group groupId=$groupId mime=$mimeType sizeBytes=${fileSize ?: -1} durationMs=$elapsedMs timeoutLike=$timeoutLike reason=$errorMessage"
                )
                if (timeoutLike) {
                    Resource.Error(
                        message = "Upload timed out on a weak network. Please retry.".asStringResourceContent()
                    )
                } else {
                    result
                }
            }
            is Resource.Loading -> result
        }
    }

    private fun isLikelyWeakNetworkError(message: String): Boolean {
        val normalized = message.trim().lowercase()
        return normalized.contains("timeout") ||
            normalized.contains("timed out") ||
            normalized.contains("network error") ||
            normalized == "retry failed"
    }

    override suspend fun reactToMessage(messageId: String, emoji: String?): Resource<Unit> =
        httpAPIUtil.callAPIWithErrorHandling<Unit, ReactToMessageResponse>(
            callAPI = {
                httpClient.post(com.dino.message.chatfeature.data.remote.ChatRoute.ReactToMessage.url) {
                    contentType(ContentType.Application.Json)
                    appendTokenToHeader(sPrefManager.getToken())
                    setBody(
                        ReactToMessageRequest(
                            messageId = messageId,
                            emoji = emoji
                        )
                    )
                }
            },
            callRefreshTokenAPI = this::getToken
        )
}
