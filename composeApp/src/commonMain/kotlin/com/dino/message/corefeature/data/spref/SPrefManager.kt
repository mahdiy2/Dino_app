package com.dino.message.corefeature.data.spref

import com.dino.message.corefeature.data.remote.util.CommonUuid
import com.dino.message.corefeature.data.util.Constants.REFRESH_TOKEN_KEY
import com.dino.message.corefeature.data.util.Constants.TOKEN_KEY
import com.dino.message.corefeature.domain.session.AuthSessionStore

/**
 * The `SPrefManager` class provides a shared preferences manager for
 * storing and retrieving data using a platform secure key-value store.
 *
 * @param secureStore The secure storage implementation supplied by the platform layer.
 */
class SPrefManager constructor(
    private val secureStore: SecureKeyValueStore
) : AuthSessionStore {
    private companion object {
        const val PRIVATE_KEY = "PRIVATE_KEY"
        const val PUBLIC_KEY = "PUBLIC_KEY"
        const val USER_LOG_IN = "USER_LOG_IN"
        const val USER_NAME = "USER_NAME"

        const val PENDING_PRIVATE_KEY = "PENDING_PRIVATE_KEY"
        const val PENDING_PUBLIC_KEY = "PENDING_PUBLIC_KEY"
        const val PENDING_REGISTRATION_ID = "PENDING_REGISTRATION_ID"
        const val PENDING_REGISTRATION_USERNAME = "PENDING_REGISTRATION_USERNAME"
        const val PENDING_REGISTRATION_NONCE = "PENDING_REGISTRATION_NONCE"
        const val PENDING_REGISTRATION_EXPIRES_AT = "PENDING_REGISTRATION_EXPIRES_AT"
        const val THREAD_REFRESH_AFTER_POST = "THREAD_REFRESH_AFTER_POST"
        const val MEDIA_SUBMISSION_SESSION_ID = "media_submission_session_id"
        const val DEVICE_SYNC_USER_ID = "device_sync_user_id"
        const val DEVICE_SYNC_DEVICE_ID = "device_sync_device_id"
    }

    /**
     * Sets the token in the shared preferences.
     *
     * @param token The token to be set.
     */
    override fun setToken(token: String) {
        secureStore.putString(TOKEN_KEY, token)
    }

    /**
     * Retrieves the token from the shared preferences.
     *
     * @return The token as a String. If the value is not found,
     * returns an empty string.
     */
    override fun getToken(): String {
        return secureStore.getString(TOKEN_KEY, "") ?: ""
    }

    /**
     * Sets the refresh token in the shared preferences.
     *
     * @param token The refresh token to be set.
     */
    override fun setRefreshToken(token: String) {
        secureStore.putString(REFRESH_TOKEN_KEY, token)
    }

    /**
     * Retrieves the refresh token from the shared preferences.
     *
     * @return The refresh token as a String. If the value is not found,
     * returns an empty string.
     */
    override fun getRefreshToken(): String {
        return secureStore.getString(REFRESH_TOKEN_KEY, "") ?: ""
    }

    override fun savePrivateKey(privateKey: String) {
        secureStore.putString(PRIVATE_KEY, privateKey)
    }

    override fun getPrivateKey(): String? {
        return secureStore.getString(PRIVATE_KEY, null)
    }

    override fun savePublicKey(publicKey: String) {
        secureStore.putString(PUBLIC_KEY, publicKey)
    }

    override fun getPublicKey(): String? {
        return secureStore.getString(PUBLIC_KEY, null)
    }

    override fun setUserLogIn(isLogIn: Boolean){
        if (!isLogIn) invalidateMediaSubmissionSession()
        secureStore.putBoolean(USER_LOG_IN, isLogIn)
    }

    override fun getUserLogIn(): Boolean {
        return secureStore.getBoolean(USER_LOG_IN, false)
    }

    override fun setUsername(username: String){
        if (getUsername() != username) invalidateMediaSubmissionSession()
        secureStore.putString(USER_NAME, username)
    }
    override fun getUsername(): String?{
        return secureStore.getString(USER_NAME, null)
    }

    override fun savePendingPrivateKey(privateKey: String) {
        secureStore.putString(PENDING_PRIVATE_KEY, privateKey)
    }

    override fun getPendingPrivateKey(): String? {
        return secureStore.getString(PENDING_PRIVATE_KEY, null)
    }

    override fun savePendingPublicKey(publicKey: String) {
        secureStore.putString(PENDING_PUBLIC_KEY, publicKey)
    }

    override fun getPendingPublicKey(): String? {
        return secureStore.getString(PENDING_PUBLIC_KEY, null)
    }

    override fun savePendingRegistrationId(registrationId: String) {
        secureStore.putString(PENDING_REGISTRATION_ID, registrationId)
    }

    override fun getPendingRegistrationId(): String? {
        return secureStore.getString(PENDING_REGISTRATION_ID, null)
    }

    override fun savePendingRegistrationUsername(username: String) {
        secureStore.putString(PENDING_REGISTRATION_USERNAME, username)
    }

    override fun getPendingRegistrationUsername(): String? {
        return secureStore.getString(PENDING_REGISTRATION_USERNAME, null)
    }

    override fun savePendingRegistrationNonce(nonce: String) {
        secureStore.putString(PENDING_REGISTRATION_NONCE, nonce)
    }

    override fun getPendingRegistrationNonce(): String? {
        return secureStore.getString(PENDING_REGISTRATION_NONCE, null)
    }

    override fun savePendingRegistrationExpiresAt(expiresAtEpochMillis: Long) {
        secureStore.putLong(PENDING_REGISTRATION_EXPIRES_AT, expiresAtEpochMillis)
    }

    override fun getPendingRegistrationExpiresAt(): Long {
        return secureStore.getLong(PENDING_REGISTRATION_EXPIRES_AT, 0L)
    }

    override fun hasPendingRegistration(): Boolean {
        return !getPendingPrivateKey().isNullOrBlank() &&
            !getPendingPublicKey().isNullOrBlank() &&
            !getPendingRegistrationId().isNullOrBlank()
    }

    override fun finalizePendingRegistration() {
        val pendingPrivateKey = getPendingPrivateKey()
        val pendingPublicKey = getPendingPublicKey()
        if (!pendingPrivateKey.isNullOrBlank()) {
            savePrivateKey(pendingPrivateKey)
        }
        if (!pendingPublicKey.isNullOrBlank()) {
            savePublicKey(pendingPublicKey)
        }
        clearPendingRegistration()
    }

    override fun clearPendingRegistration() {
        secureStore
            .remove(PENDING_PRIVATE_KEY)
            .remove(PENDING_PUBLIC_KEY)
            .remove(PENDING_REGISTRATION_ID)
            .remove(PENDING_REGISTRATION_USERNAME)
            .remove(PENDING_REGISTRATION_NONCE)
            .remove(PENDING_REGISTRATION_EXPIRES_AT)
            .apply()
    }

    override fun markThreadRefreshAfterPost() {
        secureStore.putBoolean(THREAD_REFRESH_AFTER_POST, true)
    }

    override fun consumeThreadRefreshAfterPost(): Boolean {
        val shouldRefresh = secureStore.getBoolean(THREAD_REFRESH_AFTER_POST, false)
        if (shouldRefresh) {
            secureStore.putBoolean(THREAD_REFRESH_AFTER_POST, false)
        }
        return shouldRefresh
    }

    override fun clearAuthSession() {
        invalidateMediaSubmissionSession()
        secureStore
            .remove(TOKEN_KEY)
            .remove(REFRESH_TOKEN_KEY)
            .remove(USER_NAME)
            .remove(DEVICE_SYNC_USER_ID)
            .remove(DEVICE_SYNC_DEVICE_ID)
        secureStore.putBoolean(USER_LOG_IN, false)
        secureStore
            .apply()
    }

    override fun clearAll() {
        invalidateMediaSubmissionSession()
        secureStore.clear()
    }

    var onMediaSubmissionSessionInvalidated: ((String) -> Unit)? = null

    fun mediaSubmissionSessionId(): String? {
        if (!getUserLogIn() || getUsername().isNullOrBlank()) return null
        return secureStore.getString(MEDIA_SUBMISSION_SESSION_ID, null)
            ?: CommonUuid.randomUuidString().also {
                secureStore.putString(MEDIA_SUBMISSION_SESSION_ID, it)
                secureStore.apply()
            }
    }

    fun existingMediaSubmissionSessionId(): String? =
        secureStore.getString(MEDIA_SUBMISSION_SESSION_ID, null)

    private fun invalidateMediaSubmissionSession() {
        val id = existingMediaSubmissionSessionId() ?: return
        secureStore.remove(MEDIA_SUBMISSION_SESSION_ID).apply()
        onMediaSubmissionSessionInvalidated?.invoke(id)
    }

    override fun saveDeviceSyncUserId(userId: String) {
        secureStore.putString(DEVICE_SYNC_USER_ID, userId)
    }

    override fun getDeviceSyncUserId(): String? {
        return secureStore.getString(DEVICE_SYNC_USER_ID, null)
    }

    override fun saveDeviceSyncDeviceId(deviceId: String) {
        secureStore.putString(DEVICE_SYNC_DEVICE_ID, deviceId)
    }

    override fun getDeviceSyncDeviceId(): String? {
        return secureStore.getString(DEVICE_SYNC_DEVICE_ID, null)
    }

    fun clearDeviceSyncState() {
        secureStore.remove(DEVICE_SYNC_USER_ID).remove(DEVICE_SYNC_DEVICE_ID).apply()
    }

}
