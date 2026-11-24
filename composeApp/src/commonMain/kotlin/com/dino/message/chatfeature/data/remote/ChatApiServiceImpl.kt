package com.dino.message.chatfeature.data.remote

import com.dino.message.corefeature.data.upload.currentMediaReplayRequest
import com.dino.message.chatfeature.data.remote.dto.InboxResponse
import com.dino.message.chatfeature.data.remote.dto.MessageAttachment
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
import com.dino.message.corefeature.domain.logging.AppLogger
import com.dino.message.corefeature.data.remote.util.HttpAPIUtil
import com.dino.message.corefeature.data.remote.util.appendTokenToHeader
import com.dino.message.corefeature.data.remote.util.ensureFreshAccessTokenIfNearExpiry
import com.dino.message.corefeature.domain.session.AuthSessionStore
import com.dino.message.corefeature.domain.upload.AttachmentUploadPolicy
import com.dino.message.corefeature.domain.upload.ReadableMediaFile
import com.dino.message.corefeature.data.upload.appendUploadFile
import com.dino.message.corefeature.data.upload.canOpenForUpload
import com.dino.message.corefeature.data.upload.toUploadFilePart
import com.dino.message.corefeature.domain.crypto.Base64Codec
import com.dino.message.corefeature.domain.model.AppStringKey
import com.dino.message.corefeature.domain.model.asStringResourceContent
import com.dino.message.corefeature.domain.system.AppClock
import com.dino.message.corefeature.domain.result.Resource
import com.dino.message.corefeature.domain.resource.StringResolver
import io.ktor.client.HttpClient
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.parameter
import io.ktor.client.request.setBody
import io.ktor.client.plugins.timeout
import io.ktor.http.ContentType
import io.ktor.http.contentType

class ChatApiServiceImpl constructor(
    private val httpClient: HttpClient,
    private val sPrefManager: AuthSessionStore,
    private val httpAPIUtil: HttpAPIUtil,
    private val stringResolver: StringResolver,
    private val appClock: AppClock,
    private val appLogger: AppLogger,
    private val base64Codec: Base64Codec,
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

    override suspend fun uploadAttachment(file: ReadableMediaFile): Resource<MessageAttachment> {
        val mediaReplay = currentMediaReplayRequest()
        val mimeType = AttachmentUploadPolicy.normalizeMimeType(file.handle.mimeType)
        val fileName = file.handle.displayName
        val fileSize = file.handle.sizeBytes
        val validationError = AttachmentUploadPolicy.validate(
            mimeType = mimeType,
            fileSizeBytes = fileSize,
        )
        if (validationError != null) {
            return Resource.Error(message = validationError)
        }
        if (!file.canOpenForUpload()) {
            return Resource.Error(message = AppStringKey.CannotReadFile.asStringResourceContent())
        }
        val startedAt = appClock.currentTimeMillis()
        appLogger.debug(
            TAG,
            "upload_start scope=private mime=$mimeType sizeBytes=${fileSize ?: -1} name=$fileName"
        )
        ensureFreshAccessTokenIfNearExpiry(
            sPrefManager = sPrefManager,
            refreshAccessToken = this::getToken,
            logTag = TAG,
            appClock = appClock,
            appLogger = appLogger,
            base64Codec = base64Codec,
        )

        val result = httpAPIUtil.callAPIWithErrorHandling<MessageAttachment, UploadAttachmentResponse>(
            callAPI = {
                httpClient.post(ChatRoute.UploadAttachment.url) {
                    if (mediaReplay == null) appendTokenToHeader(sPrefManager.getToken())
                    else mediaReplay.authorize(this)
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
                    "upload_success scope=private mime=$mimeType sizeBytes=${fileSize ?: -1} durationMs=$elapsedMs"
                )
                result
            }
            is Resource.Error -> {
                val errorMessage = result.message.resolveWith(stringResolver)
                val timeoutLike = isLikelyWeakNetworkError(errorMessage)
                appLogger.warning(
                    TAG,
                    "upload_failed scope=private mime=$mimeType sizeBytes=${fileSize ?: -1} durationMs=$elapsedMs timeoutLike=$timeoutLike reason=$errorMessage"
                )
                if (timeoutLike) {
                    Resource.Error(
                        message = AppStringKey.UploadTimedOutWeakNetwork.asStringResourceContent()
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
