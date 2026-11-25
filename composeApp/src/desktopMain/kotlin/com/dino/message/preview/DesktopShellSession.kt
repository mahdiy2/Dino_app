package com.dino.message.preview

import com.dino.message.corefeature.data.spref.SecureKeyValueStore
import com.dino.message.corefeature.presentation.navigation.ShellCredentials
import com.dino.message.corefeature.presentation.navigation.ShellSession
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
) : ShellSession {
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
        return username
    }

    override suspend fun signOut() {
        clearSession()
    }

    private suspend fun clearSession() {
        withContext(Dispatchers.IO) { store.remove(SESSION_KEY).apply() }
    }

    @Serializable
    private data class SavedSession(val username: String, val refreshToken: String)

    @Serializable
    private data class LoginTokens(val refresh_token: String)

    private companion object {
        const val SESSION_KEY = "desktop_auth_session"
    }
}
