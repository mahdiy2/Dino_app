package com.dino.message.corefeature.presentation.navigation

import com.dino.message.authfeature.data.remote.dto.RegisterConfirmRequest
import com.dino.message.authfeature.data.remote.dto.RegisterStartRequest
import com.dino.message.authfeature.data.remote.dto.RegisterStartResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

internal interface AccountKeyApi {
    suspend fun currentKey(): AccountPublicKey
    suspend fun rotate(password: String, publicKey: String)
}

@Serializable
internal data class AccountPublicKey(
    val username: String,
    @SerialName("public_key") val publicKey: String? = null,
)

@Serializable
internal data class RotateShellKey(
    val password: String? = null,
    @SerialName("public_key") val publicKey: String,
)

internal class ShellRegistrationApi(private val http: HttpClient) {
    suspend fun start(request: RegisterStartRequest): String {
        val response = http.post("api/auth/register/start") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
        check(response.status.isSuccess()) { "Could not start registration." }
        return response.body<RegisterStartResponse>().registration_id.also { check(it.isNotBlank()) }
    }

    suspend fun confirm(id: String): Int = http.post("api/auth/register/confirm") {
        contentType(ContentType.Application.Json)
        setBody(RegisterConfirmRequest(id))
    }.status.value
}
