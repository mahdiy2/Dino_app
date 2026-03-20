package com.dino.message.chatfeature.data.remote

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import com.dino.message.chatfeature.data.remote.dto.InboxResponse
import com.dino.message.chatfeature.data.remote.dto.MessageAttachment
import com.dino.message.chatfeature.data.remote.dto.MessageSent
import com.dino.message.chatfeature.data.remote.dto.NewMessage
import com.dino.message.chatfeature.data.remote.dto.PendingMessage
import com.dino.message.chatfeature.data.remote.dto.ReceiverPublicKeyResponse
import com.dino.message.chatfeature.data.remote.dto.SendMessageRequest
import com.dino.message.chatfeature.data.remote.dto.SendMessageResponse
import com.dino.message.chatfeature.data.remote.dto.UnreadResponse
import com.dino.message.chatfeature.data.remote.dto.UploadAttachmentResponse
import com.dino.message.chatfeature.domain.model.Inbox
import com.dino.message.chatfeature.domain.model.ReceiverPublicKey
import com.dino.message.chatfeature.domain.model.SendMessage
import com.dino.message.corefeature.data.remote.HaveRefreshTokenAPI
import com.dino.message.corefeature.data.remote.HaveRefreshTokenAPIImpl
import com.dino.message.corefeature.data.remote.util.HttpAPIUtil
import com.dino.message.corefeature.data.remote.util.appendTokenToHeader
import com.dino.message.corefeature.data.spref.SPrefManager
import com.dino.message.corefeature.domain.model.asStringResourceContent
import com.dino.message.corefeature.presentation.util.Resource
import io.ktor.client.HttpClient
import io.ktor.client.request.forms.formData
import io.ktor.client.request.forms.submitFormWithBinaryData
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import javax.inject.Inject

class ChatApiServiceImpl @Inject constructor(
    private val httpClient: HttpClient,
    private val sPrefManager: SPrefManager,
    private val httpAPIUtil: HttpAPIUtil,
    private val appContext: Context,
) : ChatApiService, HaveRefreshTokenAPI by HaveRefreshTokenAPIImpl(httpClient, sPrefManager) {

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
        val contentResolver = appContext.contentResolver
        val mimeType = resolveMimeType(fileUri)
        val fileName = resolveFileName(fileUri)
        val fileBytes = readFileBytes(fileUri)
            ?: return Resource.Error(message = "Cannot read file".asStringResourceContent())

        return httpAPIUtil.callAPIWithErrorHandling<MessageAttachment, UploadAttachmentResponse>(
            callAPI = {
                httpClient.submitFormWithBinaryData(
                    url = ChatRoute.UploadAttachment.url,
                    formData = formData {
                        append(
                            "file",
                            fileBytes,
                            Headers.build {
                                append(HttpHeaders.ContentType, mimeType)
                                append(
                                    HttpHeaders.ContentDisposition,
                                    "filename=\"$fileName\""
                                )
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
        val fromResolver = appContext.contentResolver.getType(uri)
        if (!fromResolver.isNullOrBlank() && fromResolver != "application/octet-stream") {
            return fromResolver
        }
        val extension = uri.lastPathSegment
            ?.substringAfterLast('.', "")
            ?.lowercase()
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
}
