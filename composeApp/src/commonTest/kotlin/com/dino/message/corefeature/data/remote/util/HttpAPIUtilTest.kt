package com.dino.message.corefeature.data.remote.util

import com.dino.message.corefeature.data.remote.CommonNetworkExceptionClassifier
import com.dino.message.corefeature.domain.config.AppConfig
import com.dino.message.corefeature.domain.logging.AppLogger
import com.dino.message.corefeature.domain.result.Resource
import com.dino.message.corefeature.domain.session.AuthSessionStore
import com.dino.message.corefeature.presentation.util.UiEventBus
import com.dino.message.corefeature.util.RefreshTokenExpiredException
import com.dino.message.corefeature.util.RetryableException
import io.ktor.client.plugins.HttpRequestTimeoutException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

class HttpAPIUtilTest {
    private val sessionStore = InMemoryAuthSessionStore()
    private val util = HttpAPIUtil(
        sPrefManager = sessionStore,
        networkExceptionClassifier = CommonNetworkExceptionClassifier,
        appLogger = TestAppLogger,
        uiEventBus = UiEventBus(),
        errorMessageProvider = DefaultNetworkErrorMessageProvider,
        appConfig = object : AppConfig {
            override val baseUrl = "https://example.test/"
            override val isDebug = false
            override val authPasswordRequireSymbol = false
            override val versionName = "test"
            override val versionCode = 1
            override val buildType = "test"
        },
    )

    @Test
    fun extractServerErrorMessageReadsErrorField() {
        assertEquals("Server says no", util.extractServerErrorMessage("""{"error":" Server says no "}"""))
    }

    @Test
    fun handleUnauthorizedExpiredRefreshClearsSession() = runTest {
        sessionStore.setToken("old-token")

        val result = util.handleUnauthorized<Unit>(
            callRefreshTokenAPI = { throw RefreshTokenExpiredException("expired") },
            needToUpdateToken = true,
            rawErrorBody = null,
            useServerMessageForUnauthorized = false,
        )

        assertTrue(sessionStore.authSessionCleared)
        val error = assertIs<Resource.Error<Unit>>(result)
        assertEquals(DefaultNetworkErrorMessageProvider.unauthorized(), error.message)
    }

    @Test
    fun handleUnauthorizedStoresRefreshedTokenAndRequestsRetry() = runTest {
        val error = assertFailsWith<RetryableException> {
            util.handleUnauthorized<Unit>(
                callRefreshTokenAPI = { " new-token " },
                needToUpdateToken = true,
                rawErrorBody = null,
                useServerMessageForUnauthorized = false,
            )
        }

        assertEquals("new-token", sessionStore.getToken())
        assertEquals("Token refreshed, retrying request.", error.message)
    }

    @Test
    fun retryFailureMessagePrefersTimeoutCause() {
        val message = util.resolveRetryFailureMessage(
            firstException = RetryableException("Network error"),
            lastException = RetryableException(
                message = "Timeout error",
                cause = HttpRequestTimeoutException("https://example.test/", 1000),
            ),
        )

        assertEquals(DefaultNetworkErrorMessageProvider.requestTimedOut(), message)
    }
}

private object TestAppLogger : AppLogger {
    override fun debug(tag: String, message: String) = Unit
    override fun warning(tag: String, message: String, throwable: Throwable?) = Unit
    override fun error(tag: String, message: String, throwable: Throwable?) = Unit
}

private class InMemoryAuthSessionStore : AuthSessionStore {
    private var token: String = ""
    private var refreshToken: String = ""
    var authSessionCleared: Boolean = false

    override fun setToken(token: String) {
        this.token = token
    }

    override fun getToken(): String = token
    override fun setRefreshToken(token: String) {
        refreshToken = token
    }

    override fun getRefreshToken(): String = refreshToken
    override fun savePrivateKey(privateKey: String) = Unit
    override fun getPrivateKey(): String? = null
    override fun savePublicKey(publicKey: String) = Unit
    override fun getPublicKey(): String? = null
    override fun setUserLogIn(isLogIn: Boolean) = Unit
    override fun getUserLogIn(): Boolean = false
    override fun setUsername(username: String) = Unit
    override fun getUsername(): String? = null
    override fun savePendingPrivateKey(privateKey: String) = Unit
    override fun getPendingPrivateKey(): String? = null
    override fun savePendingPublicKey(publicKey: String) = Unit
    override fun getPendingPublicKey(): String? = null
    override fun savePendingRegistrationId(registrationId: String) = Unit
    override fun getPendingRegistrationId(): String? = null
    override fun savePendingRegistrationUsername(username: String) = Unit
    override fun getPendingRegistrationUsername(): String? = null
    override fun savePendingRegistrationNonce(nonce: String) = Unit
    override fun getPendingRegistrationNonce(): String? = null
    override fun savePendingRegistrationExpiresAt(expiresAtEpochMillis: Long) = Unit
    override fun getPendingRegistrationExpiresAt(): Long = 0L
    override fun hasPendingRegistration(): Boolean = false
    override fun finalizePendingRegistration() = Unit
    override fun clearPendingRegistration() = Unit
    override fun markThreadRefreshAfterPost() = Unit
    override fun consumeThreadRefreshAfterPost(): Boolean = false
    override fun clearAuthSession() {
        authSessionCleared = true
        token = ""
        refreshToken = ""
    }

    override fun clearAll() = clearAuthSession()
    override fun saveDeviceSyncUserId(userId: String) = Unit
    override fun getDeviceSyncUserId(): String? = null
    override fun saveDeviceSyncDeviceId(deviceId: String) = Unit
    override fun getDeviceSyncDeviceId(): String? = null
}
