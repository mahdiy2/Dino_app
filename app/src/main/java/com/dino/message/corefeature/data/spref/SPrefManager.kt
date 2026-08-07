package com.dino.message.corefeature.data.spref

import com.dino.message.corefeature.data.util.Constants.REFRESH_TOKEN_KEY
import com.dino.message.corefeature.data.util.Constants.TOKEN_KEY

/**
 * The `SPrefManager` class provides a shared preferences manager for
 * storing and retrieving data using a platform secure key-value store.
 *
 * @param secureStore The secure storage implementation supplied by the platform layer.
 */
class SPrefManager constructor(
    private val secureStore: SecureKeyValueStore
) {
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
        const val DEVICE_SYNC_USER_ID = "device_sync_user_id"
        const val DEVICE_SYNC_DEVICE_ID = "device_sync_device_id"
    }

    /**
     * Sets the token in the shared preferences.
     *
     * @param token The token to be set.
     */
    fun setToken(token: String) {
        secureStore.putString(TOKEN_KEY, token)
    }

    /**
     * Retrieves the token from the shared preferences.
     *
     * @return The token as a String. If the value is not found,
     * returns an empty string.
     */
    fun getToken(): String {
        return secureStore.getString(TOKEN_KEY, "") ?: ""
    }

    /**
     * Sets the refresh token in the shared preferences.
     *
     * @param token The refresh token to be set.
     */
    fun setRefreshToken(token: String) {
        secureStore.putString(REFRESH_TOKEN_KEY, token)
    }

    /**
     * Retrieves the refresh token from the shared preferences.
     *
     * @return The refresh token as a String. If the value is not found,
     * returns an empty string.
     */
    fun getRefreshToken(): String {
        return secureStore.getString(REFRESH_TOKEN_KEY, "") ?: ""
    }

    fun savePrivateKey(privateKey: String) {
        secureStore.putString(PRIVATE_KEY, privateKey)
    }

    fun getPrivateKey(): String? {
        return secureStore.getString(PRIVATE_KEY, null)
    }

    fun savePublicKey(publicKey: String) {
        secureStore.putString(PUBLIC_KEY, publicKey)
    }

    fun getPublicKey(): String? {
        return secureStore.getString(PUBLIC_KEY, null)
    }

    fun setUserLogIn(isLogeIn: Boolean){
        secureStore.putBoolean(USER_LOG_IN, isLogeIn)
    }

    fun getUserLogIn(): Boolean {
        return secureStore.getBoolean(USER_LOG_IN, false)
    }

    fun setUsername(username: String){
        secureStore.putString(USER_NAME, username)
    }
    fun getUsername(): String?{
        return secureStore.getString(USER_NAME, null)
    }

    fun savePendingPrivateKey(privateKey: String) {
        secureStore.putString(PENDING_PRIVATE_KEY, privateKey)
    }

    fun getPendingPrivateKey(): String? {
        return secureStore.getString(PENDING_PRIVATE_KEY, null)
    }

    fun savePendingPublicKey(publicKey: String) {
        secureStore.putString(PENDING_PUBLIC_KEY, publicKey)
    }

    fun getPendingPublicKey(): String? {
        return secureStore.getString(PENDING_PUBLIC_KEY, null)
    }

    fun savePendingRegistrationId(registrationId: String) {
        secureStore.putString(PENDING_REGISTRATION_ID, registrationId)
    }

    fun getPendingRegistrationId(): String? {
        return secureStore.getString(PENDING_REGISTRATION_ID, null)
    }

    fun savePendingRegistrationUsername(username: String) {
        secureStore.putString(PENDING_REGISTRATION_USERNAME, username)
    }

    fun getPendingRegistrationUsername(): String? {
        return secureStore.getString(PENDING_REGISTRATION_USERNAME, null)
    }

    fun savePendingRegistrationNonce(nonce: String) {
        secureStore.putString(PENDING_REGISTRATION_NONCE, nonce)
    }

    fun getPendingRegistrationNonce(): String? {
        return secureStore.getString(PENDING_REGISTRATION_NONCE, null)
    }

    fun savePendingRegistrationExpiresAt(expiresAtEpochMillis: Long) {
        secureStore.putLong(PENDING_REGISTRATION_EXPIRES_AT, expiresAtEpochMillis)
    }

    fun getPendingRegistrationExpiresAt(): Long {
        return secureStore.getLong(PENDING_REGISTRATION_EXPIRES_AT, 0L)
    }

    fun hasPendingRegistration(): Boolean {
        return !getPendingPrivateKey().isNullOrBlank() &&
            !getPendingPublicKey().isNullOrBlank() &&
            !getPendingRegistrationId().isNullOrBlank()
    }

    fun finalizePendingRegistration() {
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

    fun clearPendingRegistration() {
        secureStore
            .remove(PENDING_PRIVATE_KEY)
            .remove(PENDING_PUBLIC_KEY)
            .remove(PENDING_REGISTRATION_ID)
            .remove(PENDING_REGISTRATION_USERNAME)
            .remove(PENDING_REGISTRATION_NONCE)
            .remove(PENDING_REGISTRATION_EXPIRES_AT)
            .apply()
    }

    fun markThreadRefreshAfterPost() {
        secureStore.putBoolean(THREAD_REFRESH_AFTER_POST, true)
    }

    fun consumeThreadRefreshAfterPost(): Boolean {
        val shouldRefresh = secureStore.getBoolean(THREAD_REFRESH_AFTER_POST, false)
        if (shouldRefresh) {
            secureStore.putBoolean(THREAD_REFRESH_AFTER_POST, false)
        }
        return shouldRefresh
    }

    fun clearAuthSession() {
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

    fun clearAll() {
        secureStore.clear()
    }

    fun saveDeviceSyncUserId(userId: String) {
        secureStore.putString(DEVICE_SYNC_USER_ID, userId)
    }

    fun getDeviceSyncUserId(): String? {
        return secureStore.getString(DEVICE_SYNC_USER_ID, null)
    }

    fun saveDeviceSyncDeviceId(deviceId: String) {
        secureStore.putString(DEVICE_SYNC_DEVICE_ID, deviceId)
    }

    fun getDeviceSyncDeviceId(): String? {
        return secureStore.getString(DEVICE_SYNC_DEVICE_ID, null)
    }

    fun clearDeviceSyncState() {
        secureStore.remove(DEVICE_SYNC_USER_ID).remove(DEVICE_SYNC_DEVICE_ID).apply()
    }

}
