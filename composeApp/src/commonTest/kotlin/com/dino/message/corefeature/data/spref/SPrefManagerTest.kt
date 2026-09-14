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
        manager.savePendingRegistrationExpiresAt(123L)

        assertEquals("access", store.values[Constants.TOKEN_KEY])
        assertEquals("refresh", store.values[Constants.REFRESH_TOKEN_KEY])
        assertEquals("alice", store.values["USER_NAME"])
        assertEquals(true, store.values["USER_LOG_IN"])
        assertEquals(123L, store.values["PENDING_REGISTRATION_EXPIRES_AT"])
        assertTrue(manager.hasPendingRegistration())

        manager.finalizePendingRegistration()
        assertEquals("private", manager.getPrivateKey())
        assertEquals("public", manager.getPublicKey())
        assertNull(manager.getPendingPrivateKey())
        assertNull(manager.getPendingPublicKey())
        assertNull(manager.getPendingRegistrationId())
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
