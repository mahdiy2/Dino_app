package com.dino.message.corefeature.presentation.navigation

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.serialization.Serializable

interface ShellSession {
    suspend fun restore(): String?
    suspend fun signIn(username: String, password: String): String
    suspend fun signOut()
}

internal class ShellSessionExpiredException : IllegalStateException("Sign-in required.")

@Serializable
internal data class ShellCredentials(val username: String, val password: String)

@Serializable
private data class BrowserIdentity(val username: String? = null)

internal class BrowserShellSession(private val http: HttpClient) : ShellSession, AccountKeyApi {
    override suspend fun restore() = http.get(PATH).let {
        check(it.status.isSuccess()) { "Could not check your session." }
        it.body<BrowserIdentity>().username
    }

    override suspend fun signIn(username: String, password: String): String {
        val response = http.post(PATH) {
            contentType(ContentType.Application.Json)
            setBody(ShellCredentials(username, password))
        }
        check(response.status.isSuccess()) {
            if (response.status.value in listOf(401, 403)) "Sign-in was rejected." else "Could not sign in."
        }
        return checkNotNull(response.body<BrowserIdentity>().username)
    }

    override suspend fun signOut() {
        check(http.delete(PATH).status.isSuccess()) { "Could not sign out. Try again." }
    }

    override suspend fun currentKey(): AccountPublicKey = http.get("$PATH/keys").let {
        if (it.status.value == 401) throw ShellSessionExpiredException()
        check(it.status.isSuccess()) { "Could not read the account key." }
        it.body()
    }

    override suspend fun rotate(password: String, publicKey: String) {
        val response = http.post("$PATH/keys") {
            contentType(ContentType.Application.Json)
            setBody(RotateShellKey(password, publicKey))
        }
        check(response.status.isSuccess()) { "Could not replace the account key." }
    }

    private companion object {
        const val PATH = "api/auth/web-session"
    }
}
