package com.dino.message.corefeature.domain.session

interface LocalSessionStore {
    fun getUsername(): String?
    fun getUserLogIn(): Boolean
    fun saveDeviceSyncUserId(userId: String)
    fun getDeviceSyncUserId(): String?
    fun saveDeviceSyncDeviceId(deviceId: String)
    fun getDeviceSyncDeviceId(): String?
}
