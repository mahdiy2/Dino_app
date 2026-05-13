package com.dino.message.chatfeature.data.remote

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import com.dino.message.chatfeature.data.remote.dto.InboxResponse
import com.dino.message.chatfeature.data.remote.dto.MessageAttachment
import com.dino.message.chatfeature.data.remote.dto.MessageSent
import com.dino.message.chatfeature.data.remote.dto.NewMessage
import com.dino.message.chatfeature.data.remote.dto.PendingMessage
import com.dino.message.chatfeature.data.remote.dto.PrivateHistoryResponse
import com.dino.message.chatfeature.data.remote.dto.ReactToMessageRequest
import com.dino.message.chatfeature.data.remote.dto.ReactToMessageResponse
import com.dino.message.chatfeature.data.remote.dto.ReceiverPublicKeyResponse
import com.dino.message.chatfeature.data.remote.dto.SendMessageRequest
import com.dino.message.chatfeature.data.remote.dto.SendMessageResponse
import com.dino.message.chatfeature.data.remote.dto.UnreadResponse
import com.dino.message.chatfeature.data.remote.dto.UploadAttachmentResponse
import com.dino.message.chatfeature.domain.model.Inbox
import com.dino.message.chatfeature.domain.model.PrivateHistoryPage
import com.dino.message.chatfeature.domain.model.ReceiverPublicKey
import com.dino.message.chatfeature.domain.model.SendMessage
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
import io.ktor.client.HttpClient
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.InputProvider
import io.ktor.client.request.forms.formData
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.parameter
import io.ktor.client.request.setBody
import io.ktor.client.plugins.timeout
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.utils.io.streams.asInput
import java.io.File
import javax.inject.Inject

class ChatApiServiceImpl @Inject constructor(
    private val httpClient: HttpClient,
    private val sPrefManager: SPrefManager,
    private val httpAPIUtil: HttpAPIUtil,
    private val appContext: Context,
) : ChatApiService, HaveRefreshTokenAPI by HaveRefreshTokenAPIImpl(httpClient, sPrefManager) {
    companion object {
        private const val TAG = "ChatApiServiceImpl"
        private const val ATTACHMENT_UPLOAD_TIMEOUT_MS = 120_000L
    }

    override suspend fun getInbox(): Resource<List<Inbox>?> =
        httpAPIUtil.callAPIWithErrorHandling<List<Inbox>?, InboxResponse>(
            callAPI = {
                httpClient.get(ChatRoute.GetInbox.url){
                    contentType(ContentType.Application.Json)
                    appendTokenToHeader(sPrefManager.getToken())
                }
            },
            callRefreshTokenAPI = this::getToken
        )

    override suspend fun getPrivateHistory(
        chatId: String,
        limit: Int,
        before: String?,
    ): Resource<PrivateHistoryPage> =
        httpAPIUtil.callAPIWithErrorHandling<PrivateHistoryPage, PrivateHistoryResponse>(
            callAPI = {
                httpClient.get(ChatRoute.GetPrivateHistory(chatId).url) {
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

    override suspend fun getReceiverPublicKey(username: String): Resource<ReceiverPublicKey> =
        httpAPIUtil.callAPIWithErrorHandling<ReceiverPublicKey, ReceiverPublicKeyResponse>(
            callAPI = {
                httpClient.get(ChatRoute.GetReceiverPublicKey(username).url){
                    contentType(ContentType.Application.Json)
                    appendTokenToHeader(sPrefManager.getToken())
                }
            },
            callRefreshTokenAPI = this::getToken
        )

    override suspend fun sendMessage(
        to: String,
        message: String,
        encryptedKey: String
    ): Resource<SendMessage> =
        httpAPIUtil.callAPIWithErrorHandling<SendMessage, SendMessageResponse>(
            callAPI = {
                httpClient.post(ChatRoute.SendMessage.url){
                    contentType(ContentType.Application.Json)
                    appendTokenToHeader(sPrefManager.getToken())
                    setBody(
                        SendMessageRequest(
                            to = to,
                            message = message,
                            encrypted_key = encryptedKey
                        )
                    )
                }
            },
            callRefreshTokenAPI = this::getToken
        )

    override suspend fun uploadAttachment(fileUri: Uri): Resource<MessageAttachment> {
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
            "upload_start scope=private mime=$mimeType sizeBytes=${fileSize ?: -1} name=$fileName"
        )
        ensureFreshAccessTokenIfNearExpiry(
            sPrefManager = sPrefManager,
            refreshAccessToken = this::getToken,
            logTag = TAG
        )

        val result = httpAPIUtil.callAPIWithErrorHandling<MessageAttachment, UploadAttachmentResponse>(
            callAPI = {
                httpClient.post(ChatRoute.UploadAttachment.url) {
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
                    "upload_success scope=private mime=$mimeType sizeBytes=${fileSize ?: -1} durationMs=$elapsedMs"
                )
                result
            }
            is Resource.Error -> {
                val errorMessage = result.message.asString(appContext)
                val timeoutLike = isLikelyWeakNetworkError(errorMessage)
                Log.w(
                    TAG,
                    "upload_failed scope=private mime=$mimeType sizeBytes=${fileSize ?: -1} durationMs=$elapsedMs timeoutLike=$timeoutLike reason=$errorMessage"
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
        if (uri.scheme == "file") {
            return uri.lastPathSegment ?: "file"
        }
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

    override suspend fun getUnread(): Resource<UnreadResponse> =
        httpAPIUtil.callAPIWithErrorHandling<UnreadResponse, UnreadResponse>(
            callAPI = {
                httpClient.get(ChatRoute.GetUnread.url) {
                    contentType(ContentType.Application.Json)
                    appendTokenToHeader(sPrefManager.getToken())
                }
            },
            callRefreshTokenAPI = this::getToken
        )

    override suspend fun reactToMessage(messageId: String, emoji: String?): Resource<Unit> =
        httpAPIUtil.callAPIWithErrorHandling<Unit, ReactToMessageResponse>(
            callAPI = {
                httpClient.post(ChatRoute.ReactToMessage.url) {
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
