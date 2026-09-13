package com.dino.message.updatefeature.presentation.viewmodel

import com.dino.message.corefeature.domain.session.AuthSessionStore
import com.dino.message.corefeature.presentation.navigation.AppRoute
import com.dino.message.corefeature.presentation.util.PopBackStackLevel
import com.dino.message.corefeature.presentation.util.UIEvent
import com.dino.message.corefeature.presentation.util.UiEventBus
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class AppUpdateViewModelTest {
    private val mainDispatcher = UnconfinedTestDispatcher()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(mainDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun continueWithoutUpdate_routesToThreadWhenLoggedIn() = runTest {
        val viewModel = AppUpdateViewModel(
            uiEventBus = UiEventBus(),
            sPrefManager = FakeAuthSessionStore(userLoggedIn = true),
        )
        try {
            val emitted = async(start = CoroutineStart.UNDISPATCHED) {
                viewModel.uIEventFlow.first()
            }

            viewModel.continueWithoutUpdate()

            assertEquals(
                UIEvent.NavigateWithPopBackStack(AppRoute.Thread, PopBackStackLevel.ALL),
                emitted.await(),
            )
        } finally {
            viewModel.clear()
        }
    }

    private class FakeAuthSessionStore(
        private var userLoggedIn: Boolean = false,
        private var pendingRegistration: Boolean = false,
    ) : AuthSessionStore {
        private val strings = mutableMapOf<String, String?>()
        private var pendingRegistrationExpiresAt: Long = 0L
        private var refreshAfterPost = false

        override fun setToken(token: String) {
            strings["token"] = token
        }

        override fun getToken(): String = strings["token"].orEmpty()

        override fun setRefreshToken(token: String) {
            strings["refreshToken"] = token
        }

        override fun getRefreshToken(): String = strings["refreshToken"].orEmpty()

        override fun savePrivateKey(privateKey: String) {
            strings["privateKey"] = privateKey
        }

        override fun getPrivateKey(): String? = strings["privateKey"]

        override fun savePublicKey(publicKey: String) {
            strings["publicKey"] = publicKey
        }

        override fun getPublicKey(): String? = strings["publicKey"]

        override fun setUserLogIn(isLogIn: Boolean) {
            userLoggedIn = isLogIn
        }

        override fun getUserLogIn(): Boolean = userLoggedIn

        override fun setUsername(username: String) {
            strings["username"] = username
        }

        override fun getUsername(): String? = strings["username"]

        override fun savePendingPrivateKey(privateKey: String) {
            strings["pendingPrivateKey"] = privateKey
        }

        override fun getPendingPrivateKey(): String? = strings["pendingPrivateKey"]

        override fun savePendingPublicKey(publicKey: String) {
            strings["pendingPublicKey"] = publicKey
        }

        override fun getPendingPublicKey(): String? = strings["pendingPublicKey"]

        override fun savePendingRegistrationId(registrationId: String) {
            pendingRegistration = true
            strings["pendingRegistrationId"] = registrationId
        }

        override fun getPendingRegistrationId(): String? = strings["pendingRegistrationId"]

        override fun savePendingRegistrationUsername(username: String) {
            strings["pendingRegistrationUsername"] = username
        }

        override fun getPendingRegistrationUsername(): String? = strings["pendingRegistrationUsername"]

        override fun savePendingRegistrationNonce(nonce: String) {
            strings["pendingRegistrationNonce"] = nonce
        }

        override fun getPendingRegistrationNonce(): String? = strings["pendingRegistrationNonce"]

        override fun savePendingRegistrationExpiresAt(expiresAtEpochMillis: Long) {
            pendingRegistrationExpiresAt = expiresAtEpochMillis
        }

        override fun getPendingRegistrationExpiresAt(): Long = pendingRegistrationExpiresAt

        override fun hasPendingRegistration(): Boolean = pendingRegistration

        override fun finalizePendingRegistration() {
            pendingRegistration = false
        }

        override fun clearPendingRegistration() {
            pendingRegistration = false
            strings.remove("pendingPrivateKey")
            strings.remove("pendingPublicKey")
            strings.remove("pendingRegistrationId")
            strings.remove("pendingRegistrationUsername")
            strings.remove("pendingRegistrationNonce")
            pendingRegistrationExpiresAt = 0L
        }

        override fun markThreadRefreshAfterPost() {
            refreshAfterPost = true
        }

        override fun consumeThreadRefreshAfterPost(): Boolean {
            val current = refreshAfterPost
            refreshAfterPost = false
            return current
        }

        override fun clearAuthSession() {
            userLoggedIn = false
            strings.remove("token")
            strings.remove("refreshToken")
        }

        override fun clearAll() {
            strings.clear()
            userLoggedIn = false
            pendingRegistration = false
            pendingRegistrationExpiresAt = 0L
            refreshAfterPost = false
        }

        override fun saveDeviceSyncUserId(userId: String) {
            strings["deviceSyncUserId"] = userId
        }

        override fun getDeviceSyncUserId(): String? = strings["deviceSyncUserId"]

        override fun saveDeviceSyncDeviceId(deviceId: String) {
            strings["deviceSyncDeviceId"] = deviceId
        }

        override fun getDeviceSyncDeviceId(): String? = strings["deviceSyncDeviceId"]
    }
}
