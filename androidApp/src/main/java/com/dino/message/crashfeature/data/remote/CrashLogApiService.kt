package com.dino.message.crashfeature.data.remote

import com.dino.message.corefeature.data.remote.util.appendTokenToHeader
import com.dino.message.corefeature.data.spref.SPrefManager
import com.dino.message.crashfeature.data.remote.dto.CrashLogUploadRequest
import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess

class CrashLogApiService constructor(
    private val httpClient: HttpClient,
    private val sPrefManager: SPrefManager,
) {
    suspend fun uploadCrashLog(payload: CrashLogUploadRequest): Boolean {
        return runCatching {
            val response = httpClient.post(CrashLogRoute.UploadCrashLog.url) {
                contentType(ContentType.Application.Json)
                appendTokenToHeader(sPrefManager.getToken())
                setBody(payload)
            }
            response.status.isSuccess()
        }.getOrDefault(false)
    }
}
