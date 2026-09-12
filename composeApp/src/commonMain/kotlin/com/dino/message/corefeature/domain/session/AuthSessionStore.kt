package com.dino.message.corefeature.domain.session

interface AuthSessionStore {
    fun setToken(token: String)
    fun getToken(): String

    fun setRefreshToken(token: String)
    fun getRefreshToken(): String

    fun savePrivateKey(privateKey: String)
    fun getPrivateKey(): String?

    fun savePublicKey(publicKey: String)
    fun getPublicKey(): String?

    fun setUserLogIn(isLogIn: Boolean)
    fun getUserLogIn(): Boolean

    fun setUsername(username: String)
    fun getUsername(): String?

    fun savePendingPrivateKey(privateKey: String)
    fun getPendingPrivateKey(): String?

    fun savePendingPublicKey(publicKey: String)
    fun getPendingPublicKey(): String?

    fun savePendingRegistrationId(registrationId: String)
    fun getPendingRegistrationId(): String?

    fun savePendingRegistrationUsername(username: String)
    fun getPendingRegistrationUsername(): String?

    fun savePendingRegistrationNonce(nonce: String)
    fun getPendingRegistrationNonce(): String?

    fun savePendingRegistrationExpiresAt(expiresAtEpochMillis: Long)
    fun getPendingRegistrationExpiresAt(): Long

    fun hasPendingRegistration(): Boolean
    fun finalizePendingRegistration()
    fun clearPendingRegistration()

    fun markThreadRefreshAfterPost()
    fun consumeThreadRefreshAfterPost(): Boolean

    fun clearAuthSession()
    fun clearAll()

    fun saveDeviceSyncUserId(userId: String)
    fun getDeviceSyncUserId(): String?

    fun saveDeviceSyncDeviceId(deviceId: String)
    fun getDeviceSyncDeviceId(): String?
}
