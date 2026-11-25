package com.dino.message.preview

import com.dino.message.corefeature.data.spref.SecureKeyValueStore
import com.dino.message.corefeature.presentation.navigation.AccountKeyApi
import com.dino.message.corefeature.presentation.navigation.AccountPublicKey
import com.dino.message.corefeature.presentation.navigation.RotateShellKey
import com.dino.message.corefeature.presentation.navigation.ShellCredentials
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import com.dino.message.corefeature.presentation.navigation.ShellSession
import com.dino.message.corefeature.presentation.navigation.ShellSessionExpiredException
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

internal class DesktopShellSession(
    private val http: HttpClient,
    private val store: SecureKeyValueStore,
) : ShellSession, AccountKeyApi {
    private val refreshLock = Mutex()
    private var accessToken: String? = null

    override suspend fun restore(): String? {
        val saved = withContext(Dispatchers.IO) {
            store.getString(SESSION_KEY)?.let { Json.decodeFromString<SavedSession>(it) }
        } ?: return null
        val response = http.post("api/auth/refresh") { bearerAuth(saved.refreshToken) }
        if (response.status.value in setOf(401, 403, 422)) {
            clearSession()
            return null
        }
        check(response.status.isSuccess()) { "Could not restore session" }
        accessToken = response.body<AccessToken>().access_token
        return saved.username
    }

    override suspend fun signIn(username: String, password: String): String {
        val response = http.post("api/auth/login") {
            contentType(ContentType.Application.Json)
            setBody(ShellCredentials(username, password))
        }
        check(response.status.isSuccess()) { "Sign-in failed" }
        val tokens = response.body<LoginTokens>()
        check(tokens.refresh_token.isNotBlank()) { "Missing refresh token" }
        withContext(Dispatchers.IO) {
            store.putString(SESSION_KEY, Json.encodeToString(SavedSession(username, tokens.refresh_token)))
        }
        accessToken = tokens.access_token
        return username
    }

    override suspend fun signOut() {
        clearSession()
    }

    private suspend fun clearSession() {
        accessToken = null
        withContext(Dispatchers.IO) { store.remove(SESSION_KEY).apply() }
    }

    override suspend fun currentKey(): AccountPublicKey {
        val response = authenticated { token ->
            http.get("api/auth/keys/status?include_public_key=1") { bearerAuth(token) }
        }
        check(response.status.isSuccess()) { "Could not read the account key." }
        return response.body()
    }

    override suspend fun rotate(password: String, publicKey: String) {
        val account = currentKey().username
        val credentials = http.post("api/auth/login") {
            contentType(ContentType.Application.Json)
            setBody(ShellCredentials(account, password))
        }
        check(credentials.status.isSuccess()) { "Password verification failed." }
        val confirmedToken = credentials.body<LoginTokens>().access_token
        val response = http.post("api/auth/keys/rotate") {
            bearerAuth(confirmedToken)
            contentType(ContentType.Application.Json)
            setBody(RotateShellKey(publicKey = publicKey))
        }
        check(response.status.isSuccess()) { "Could not replace the account key." }
    }

    private suspend fun authenticated(request: suspend (String) -> HttpResponse): HttpResponse {
        val previous = refreshLock.withLock {
            accessToken ?: restore()?.let { accessToken }
        } ?: throw ShellSessionExpiredException()
        val response = request(previous)
        if (response.status.value != 401) return response
        val refreshed = refreshLock.withLock {
            if (accessToken == previous) restore()
            accessToken
        } ?: throw ShellSessionExpiredException()
        return request(refreshed)
    }

    @Serializable
    private data class AccessToken(val access_token: String)

    @Serializable
    private data class SavedSession(val username: String, val refreshToken: String)

    @Serializable
    private data class LoginTokens(val refresh_token: String, val access_token: String)

    private companion object {
        const val SESSION_KEY = "desktop_auth_session"
    }
}
