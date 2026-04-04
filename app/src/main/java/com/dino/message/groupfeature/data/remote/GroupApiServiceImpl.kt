package com.dino.message.groupfeature.data.remote

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import com.dino.message.chatfeature.data.remote.dto.MessageAttachment
import com.dino.message.corefeature.data.remote.HaveRefreshTokenAPI
import com.dino.message.corefeature.data.remote.HaveRefreshTokenAPIImpl
import com.dino.message.corefeature.data.remote.util.HttpAPIUtil
import com.dino.message.corefeature.data.remote.util.appendTokenToHeader
import com.dino.message.corefeature.data.spref.SPrefManager
import com.dino.message.corefeature.domain.model.asStringResourceContent
import com.dino.message.corefeature.presentation.util.Resource
import com.dino.message.groupfeature.data.remote.dto.AddGroupMembersRequest
import com.dino.message.groupfeature.data.remote.dto.AddGroupMembersResponse
import com.dino.message.groupfeature.data.remote.dto.CreateGroupRequest
import com.dino.message.groupfeature.data.remote.dto.CreateGroupResponse
import com.dino.message.groupfeature.data.remote.dto.GroupDetailResponse
import com.dino.message.groupfeature.data.remote.dto.GroupUploadAttachmentResponse
import com.dino.message.groupfeature.data.remote.dto.ListGroupsResponse
import com.dino.message.groupfeature.data.remote.dto.MutualFollowersResponse
import com.dino.message.groupfeature.domain.model.AddGroupMembersResult
import com.dino.message.groupfeature.domain.model.Group
import com.dino.message.groupfeature.domain.model.MutualFollowersPage
import io.ktor.client.HttpClient
import io.ktor.client.request.forms.formData
import io.ktor.client.request.forms.submitFormWithBinaryData
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import javax.inject.Inject

class GroupApiServiceImpl @Inject constructor(
    private val httpClient: HttpClient,
    private val sPrefManager: SPrefManager,
    private val httpAPIUtil: HttpAPIUtil,
    private val appContext: Context,
) : GroupApiService, HaveRefreshTokenAPI by HaveRefreshTokenAPIImpl(httpClient, sPrefManager) {

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

    override suspend fun uploadGroupAttachment(groupId: Int, fileUri: Uri): Resource<MessageAttachment> {
        val mimeType = resolveMimeType(fileUri)
        val fileName = resolveFileName(fileUri)
        val fileBytes = readFileBytes(fileUri)
            ?: return Resource.Error(message = "Cannot read file".asStringResourceContent())

        return httpAPIUtil.callAPIWithErrorHandling<MessageAttachment, GroupUploadAttachmentResponse>(
            callAPI = {
                httpClient.submitFormWithBinaryData(
                    url = GroupRoute.GroupAttachments(groupId).url,
                    formData = formData {
                        append(
                            "file",
                            fileBytes,
                            Headers.build {
                                append(HttpHeaders.ContentType, mimeType)
                                append(HttpHeaders.ContentDisposition, "filename=\"$fileName\"")
                            }
                        )
                    }
                ) {
                    appendTokenToHeader(sPrefManager.getToken())
                }
            },
            callRefreshTokenAPI = this::getToken
        )
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
        val fromResolver = appContext.contentResolver.getType(uri)
        if (!fromResolver.isNullOrBlank() && fromResolver != "application/octet-stream") return fromResolver
        val extension = uri.lastPathSegment?.substringAfterLast('.', "")?.lowercase()
        if (!extension.isNullOrBlank()) {
            val fromExtension = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
            if (!fromExtension.isNullOrBlank()) return fromExtension
        }
        return fromResolver ?: "application/octet-stream"
    }

    private fun readFileBytes(uri: Uri): ByteArray? {
        if (uri.scheme == "file") {
            val path = uri.path ?: return null
            val file = java.io.File(path)
            return if (file.exists()) file.readBytes() else null
        }
        return appContext.contentResolver.openInputStream(uri)?.use { it.readBytes() }
    }
}
