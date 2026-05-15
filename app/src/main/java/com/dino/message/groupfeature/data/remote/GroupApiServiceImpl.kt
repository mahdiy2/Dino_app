package com.dino.message.groupfeature.data.remote

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import com.dino.message.chatfeature.data.remote.dto.MessageAttachment
import com.dino.message.corefeature.data.remote.HaveRefreshTokenAPI
import com.dino.message.corefeature.data.remote.HaveRefreshTokenAPIImpl
import com.dino.message.corefeature.data.remote.util.HttpAPIUtil
import com.dino.message.corefeature.data.remote.util.appendTokenToHeader
import com.dino.message.corefeature.data.remote.util.ensureFreshAccessTokenIfNearExpiry
import com.dino.message.corefeature.data.spref.SPrefManager
import com.dino.message.corefeature.data.upload.AttachmentUploadPolicy
import com.dino.message.corefeature.data.upload.MediaFileResolver
import com.dino.message.corefeature.domain.model.asStringResourceContent
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
import io.ktor.client.request.forms.InputProvider
import io.ktor.client.request.forms.formData
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.plugins.timeout
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.utils.io.streams.asInput
import java.io.File
import javax.inject.Inject

class GroupApiServiceImpl @Inject constructor(
    private val httpClient: HttpClient,
    private val sPrefManager: SPrefManager,
    private val httpAPIUtil: HttpAPIUtil,
    private val appContext: Context,
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

    override suspend fun uploadGroupAttachment(groupId: Int, fileUri: Uri): Resource<MessageAttachment> {
        val mimeType = AttachmentUploadPolicy.normalizeMimeType(resolveMimeType(fileUri))
        val fileName = resolveFileName(fileUri)
        val fileSize = resolveFileSize(fileUri)
        val validationError = AttachmentUploadPolicy.validate(
            mimeType = mimeType,
            fileSizeBytes = fileSize,
        )
        if (validationError != null) {
            return Resource.Error(message = validationError.asStringResourceContent())
        }
        if (!canReadFile(fileUri)) {
            return Resource.Error(message = "Cannot read file".asStringResourceContent())
        }
        val startedAt = System.currentTimeMillis()
        Log.i(
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
                                append(
                                    key = "file",
                                    value = InputProvider(size = fileSize) {
                                        openInputStream(fileUri).asInput()
                                    },
                                    headers = Headers.build {
                                        append(HttpHeaders.ContentType, mimeType)
                                        append(
                                            HttpHeaders.ContentDisposition,
                                            "form-data; name=\"file\"; filename=\"$fileName\""
                                        )
                                    },
                                )
                            }
                        )
                    )
                }
            },
            callRefreshTokenAPI = this::getToken
        )
        val elapsedMs = System.currentTimeMillis() - startedAt
        return when (result) {
            is Resource.Success -> {
                Log.i(
                    TAG,
                    "upload_success scope=group groupId=$groupId mime=$mimeType sizeBytes=${fileSize ?: -1} durationMs=$elapsedMs"
                )
                result
            }
            is Resource.Error -> {
                val errorMessage = result.message.asString(appContext)
                val timeoutLike = isLikelyWeakNetworkError(errorMessage)
                Log.w(
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

    private fun resolveFileName(uri: Uri): String {
        if (uri.scheme == "file") return uri.lastPathSegment ?: "file"
        val cursor = appContext.contentResolver.query(uri, null, null, null, null)
            ?: return uri.lastPathSegment ?: "file"
        return cursor.use {
            if (it.moveToFirst()) {
                val index = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index >= 0) it.getString(index) else uri.lastPathSegment ?: "file"
            } else uri.lastPathSegment ?: "file"
        }
    }

    private fun resolveMimeType(uri: Uri): String {
        return MediaFileResolver.resolveMimeType(
            contentResolver = appContext.contentResolver,
            uri = uri,
            displayNameHint = resolveFileName(uri),
        )
    }

    private fun resolveFileSize(uri: Uri): Long? {
        return MediaFileResolver.resolveFileSize(appContext.contentResolver, uri)
    }

    private fun canReadFile(uri: Uri): Boolean {
        return runCatching {
            openInputStream(uri).use { }
            true
        }.getOrDefault(false)
    }

    private fun openInputStream(uri: Uri): java.io.InputStream {
        if (uri.scheme == "file") {
            val path = uri.path ?: error("File path is missing")
            val file = File(path)
            if (!file.exists()) error("File does not exist")
            return file.inputStream()
        }
        return appContext.contentResolver.openInputStream(uri)
            ?: error("Unable to open input stream")
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
