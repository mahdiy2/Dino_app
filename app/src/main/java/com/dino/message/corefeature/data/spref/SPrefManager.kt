package com.dino.message.corefeature.data.spref

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.dino.message.corefeature.data.util.Constants.REFRESH_TOKEN_KEY
import com.dino.message.corefeature.data.util.Constants.SEC_MESSAGE_SHARED_PREFERENCES_NAME
import com.dino.message.corefeature.data.util.Constants.TOKEN_KEY
import com.google.gson.Gson

/**
 * The `SPrefManager` class provides a shared preferences manager for
 * storing and retrieving data using SharedPreferences in a secure manner.
 *
 * @param context The `Context` object used for accessing resources.
 */
class SPrefManager constructor(
    private val gson: Gson,
    context: Context
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

    private val masterKey: MasterKey =
        MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()

    private val sPref = EncryptedSharedPreferences.create(
        context,
        SEC_MESSAGE_SHARED_PREFERENCES_NAME,
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    private val editor = sPref.edit()

    /**
     * Sets the token in the shared preferences.
     *
     * @param token The token to be set.
     */
    fun setToken(token: String) {
        editor.putString(TOKEN_KEY, token).apply()
    }

    /**
     * Retrieves the token from the shared preferences.
     *
     * @return The token as a String. If the value is not found,
     * returns an empty string.
     */
    fun getToken(): String {
        return sPref.getString(TOKEN_KEY, "") ?: ""
    }

    /**
     * Sets the refresh token in the shared preferences.
     *
     * @param token The refresh token to be set.
     */
    fun setRefreshToken(token: String) {
        editor.putString(REFRESH_TOKEN_KEY, token).apply()
    }

    /**
     * Retrieves the refresh token from the shared preferences.
     *
     * @return The refresh token as a String. If the value is not found,
     * returns an empty string.
     */
    fun getRefreshToken(): String {
        return sPref.getString(REFRESH_TOKEN_KEY, "") ?: ""
    }

    fun savePrivateKey(privateKey: String) {
        editor.putString(PRIVATE_KEY, privateKey).apply()
    }

    fun getPrivateKey(): String? {
        return sPref.getString(PRIVATE_KEY, null)
    }

    fun savePublicKey(publicKey: String) {
        editor.putString(PUBLIC_KEY, publicKey).apply()
    }

    fun getPublicKey(): String? {
        return sPref.getString(PUBLIC_KEY, null)
    }

    fun setUserLogIn(isLogeIn: Boolean){
        editor.putBoolean(USER_LOG_IN, isLogeIn).apply()
    }

    fun getUserLogIn(): Boolean {
        return sPref.getBoolean(USER_LOG_IN, false)
    }

    fun setUsername(username: String){
        editor.putString(USER_NAME, username).apply()
    }
    fun getUsername(): String?{
        return sPref.getString(USER_NAME, null)
    }

    fun savePendingPrivateKey(privateKey: String) {
        editor.putString(PENDING_PRIVATE_KEY, privateKey).apply()
    }

    fun getPendingPrivateKey(): String? {
        return sPref.getString(PENDING_PRIVATE_KEY, null)
    }

    fun savePendingPublicKey(publicKey: String) {
        editor.putString(PENDING_PUBLIC_KEY, publicKey).apply()
    }

    fun getPendingPublicKey(): String? {
        return sPref.getString(PENDING_PUBLIC_KEY, null)
    }

    fun savePendingRegistrationId(registrationId: String) {
        editor.putString(PENDING_REGISTRATION_ID, registrationId).apply()
    }

    fun getPendingRegistrationId(): String? {
        return sPref.getString(PENDING_REGISTRATION_ID, null)
    }

    fun savePendingRegistrationUsername(username: String) {
        editor.putString(PENDING_REGISTRATION_USERNAME, username).apply()
    }

    fun getPendingRegistrationUsername(): String? {
        return sPref.getString(PENDING_REGISTRATION_USERNAME, null)
    }

    fun savePendingRegistrationNonce(nonce: String) {
        editor.putString(PENDING_REGISTRATION_NONCE, nonce).apply()
    }

    fun getPendingRegistrationNonce(): String? {
        return sPref.getString(PENDING_REGISTRATION_NONCE, null)
    }

    fun savePendingRegistrationExpiresAt(expiresAtEpochMillis: Long) {
        editor.putLong(PENDING_REGISTRATION_EXPIRES_AT, expiresAtEpochMillis).apply()
    }

    fun getPendingRegistrationExpiresAt(): Long {
        return sPref.getLong(PENDING_REGISTRATION_EXPIRES_AT, 0L)
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
        editor
            .remove(PENDING_PRIVATE_KEY)
            .remove(PENDING_PUBLIC_KEY)
            .remove(PENDING_REGISTRATION_ID)
            .remove(PENDING_REGISTRATION_USERNAME)
            .remove(PENDING_REGISTRATION_NONCE)
            .remove(PENDING_REGISTRATION_EXPIRES_AT)
            .apply()
    }

    fun markThreadRefreshAfterPost() {
        editor.putBoolean(THREAD_REFRESH_AFTER_POST, true).apply()
    }

    fun consumeThreadRefreshAfterPost(): Boolean {
        val shouldRefresh = sPref.getBoolean(THREAD_REFRESH_AFTER_POST, false)
        if (shouldRefresh) {
            editor.putBoolean(THREAD_REFRESH_AFTER_POST, false).apply()
        }
        return shouldRefresh
    }

    fun clearAuthSession() {
        editor
            .remove(TOKEN_KEY)
            .remove(REFRESH_TOKEN_KEY)
            .remove(USER_NAME)
            .remove(DEVICE_SYNC_USER_ID)
            .remove(DEVICE_SYNC_DEVICE_ID)
            .putBoolean(USER_LOG_IN, false)
            .apply()
    }

    fun clearAll() {
        editor.clear().apply()
    }

    fun saveDeviceSyncUserId(userId: String) {
        editor.putString(DEVICE_SYNC_USER_ID, userId).apply()
    }

    fun getDeviceSyncUserId(): String? {
        return sPref.getString(DEVICE_SYNC_USER_ID, null)
    }

    fun saveDeviceSyncDeviceId(deviceId: String) {
        editor.putString(DEVICE_SYNC_DEVICE_ID, deviceId).apply()
    }

    fun getDeviceSyncDeviceId(): String? {
        return sPref.getString(DEVICE_SYNC_DEVICE_ID, null)
    }

    fun clearDeviceSyncState() {
        editor.remove(DEVICE_SYNC_USER_ID).remove(DEVICE_SYNC_DEVICE_ID).apply()
    }

}
