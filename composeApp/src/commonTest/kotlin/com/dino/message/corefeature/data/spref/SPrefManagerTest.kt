package com.dino.message.corefeature.data.spref

import com.dino.message.corefeature.data.util.Constants
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SPrefManagerTest {
    @Test
    fun preservesExistingKeysDefaultsAndPendingRegistrationFlow() {
        val store = RecordingSecureKeyValueStore()
        val manager = SPrefManager(store)

        assertEquals("", manager.getToken())
        assertEquals("", manager.getRefreshToken())
        assertFalse(manager.getUserLogIn())
        assertEquals(0L, manager.getPendingRegistrationExpiresAt())
        assertFalse(manager.hasPendingRegistration())

        manager.setToken("access")
        manager.setRefreshToken("refresh")
        manager.setUsername("alice")
        manager.setUserLogIn(true)
        manager.savePendingPrivateKey("private")
        manager.savePendingPublicKey("public")
        manager.savePendingRegistrationId("registration")
        manager.savePendingRegistrationUsername("pending-alice")
        manager.savePendingRegistrationNonce("nonce")
        manager.savePendingRegistrationExpiresAt(123L)
        manager.saveDeviceSyncUserId("sync-user")
        manager.saveDeviceSyncDeviceId("sync-device")
        manager.markThreadRefreshAfterPost()

        assertEquals("access", store.values[Constants.TOKEN_KEY])
        assertEquals("refresh", store.values[Constants.REFRESH_TOKEN_KEY])
        assertEquals("alice", store.values["USER_NAME"])
        assertEquals(true, store.values["USER_LOG_IN"])
        assertEquals("private", store.values["PENDING_PRIVATE_KEY"])
        assertEquals("public", store.values["PENDING_PUBLIC_KEY"])
        assertEquals("registration", store.values["PENDING_REGISTRATION_ID"])
        assertEquals("pending-alice", store.values["PENDING_REGISTRATION_USERNAME"])
        assertEquals("nonce", store.values["PENDING_REGISTRATION_NONCE"])
        assertEquals(123L, store.values["PENDING_REGISTRATION_EXPIRES_AT"])
        assertEquals("sync-user", store.values["device_sync_user_id"])
        assertEquals("sync-device", store.values["device_sync_device_id"])
        assertEquals(true, store.values["THREAD_REFRESH_AFTER_POST"])
        assertTrue(manager.hasPendingRegistration())
        assertTrue(manager.consumeThreadRefreshAfterPost())
        assertFalse(manager.consumeThreadRefreshAfterPost())

        manager.finalizePendingRegistration()
        assertEquals("private", manager.getPrivateKey())
        assertEquals("public", manager.getPublicKey())
        assertNull(manager.getPendingPrivateKey())
        assertNull(manager.getPendingPublicKey())
        assertNull(manager.getPendingRegistrationId())
        assertNull(manager.getPendingRegistrationUsername())
        assertNull(manager.getPendingRegistrationNonce())
        assertEquals(0L, manager.getPendingRegistrationExpiresAt())
    }

    @Test
    fun logoutPreservesLongTermKeysAndClearsSessionAndDeviceState() {
        val store = RecordingSecureKeyValueStore()
        val manager = SPrefManager(store)
        manager.savePrivateKey("private")
        manager.savePublicKey("public")
        manager.setToken("access")
        manager.setRefreshToken("refresh")
        manager.setUsername("alice")
        manager.setUserLogIn(true)
        manager.saveDeviceSyncUserId("user")
        manager.saveDeviceSyncDeviceId("device")

        manager.clearAuthSession()

        assertEquals("private", manager.getPrivateKey())
        assertEquals("public", manager.getPublicKey())
        assertEquals("", manager.getToken())
        assertEquals("", manager.getRefreshToken())
        assertNull(manager.getUsername())
        assertNull(manager.getDeviceSyncUserId())
        assertNull(manager.getDeviceSyncDeviceId())
        assertFalse(manager.getUserLogIn())
    }

    @Test
    fun clearAllRemovesLongTermAndSessionValues() {
        val store = RecordingSecureKeyValueStore()
        val manager = SPrefManager(store)
        manager.savePrivateKey("private")
        manager.savePublicKey("public")
        manager.setToken("access")
        manager.setRefreshToken("refresh")
        manager.setUserLogIn(true)

        manager.clearAll()

        assertTrue(store.values.isEmpty())
        assertNull(manager.getPrivateKey())
        assertNull(manager.getPublicKey())
        assertEquals("", manager.getToken())
        assertEquals("", manager.getRefreshToken())
        assertFalse(manager.getUserLogIn())
    }

    private class RecordingSecureKeyValueStore : SecureKeyValueStore {
        val values = mutableMapOf<String, Any>()
        override fun putString(key: String, value: String) { values[key] = value }
        override fun getString(key: String, defaultValue: String?): String? = values[key] as? String ?: defaultValue
        override fun putBoolean(key: String, value: Boolean) { values[key] = value }
        override fun getBoolean(key: String, defaultValue: Boolean): Boolean = values[key] as? Boolean ?: defaultValue
        override fun putLong(key: String, value: Long) { values[key] = value }
        override fun getLong(key: String, defaultValue: Long): Long = values[key] as? Long ?: defaultValue
        override fun remove(key: String): SecureKeyValueStore = apply { values.remove(key) }
        override fun clear() { values.clear() }
        override fun apply() = Unit
    }
}
