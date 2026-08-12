package com.dino.message.corefeature.data.session

import com.dino.message.corefeature.data.spref.SPrefManager
import com.dino.message.corefeature.domain.session.LocalSessionCleaner
import com.dino.message.corefeature.domain.session.LocalSessionStore

class SPrefLocalSessionStore(
    private val sPrefManager: SPrefManager,
) : LocalSessionStore, LocalSessionCleaner {
    override fun getUsername(): String? = sPrefManager.getUsername()

    override fun getUserLogIn(): Boolean = sPrefManager.getUserLogIn()

    override fun saveDeviceSyncUserId(userId: String) {
        sPrefManager.saveDeviceSyncUserId(userId)
    }

    override fun getDeviceSyncUserId(): String? = sPrefManager.getDeviceSyncUserId()

    override fun saveDeviceSyncDeviceId(deviceId: String) {
        sPrefManager.saveDeviceSyncDeviceId(deviceId)
    }

    override fun getDeviceSyncDeviceId(): String? = sPrefManager.getDeviceSyncDeviceId()

    override fun clearAll() {
        sPrefManager.clearAll()
    }
}
