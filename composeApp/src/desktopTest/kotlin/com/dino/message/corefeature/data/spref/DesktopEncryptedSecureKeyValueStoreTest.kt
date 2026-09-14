package com.dino.message.corefeature.data.spref

import java.nio.file.Files
import kotlin.io.path.readBytes
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DesktopEncryptedSecureKeyValueStoreTest {
    @Test
    fun persistsEncryptedTypedValuesAndFailsClosedWhenCorrupt() {
        val directory = Files.createTempDirectory("secure-store-test")
        try {
            val store = DesktopEncryptedSecureKeyValueStore(directory)
            store.putString("TOKEN", "secret-access-token")
            store.putBoolean("USER_LOG_IN", true)
            store.putLong("PENDING_REGISTRATION_EXPIRES_AT", 42L)

            val reopened = DesktopEncryptedSecureKeyValueStore(directory)
            assertEquals("secret-access-token", reopened.getString("TOKEN"))
            assertTrue(reopened.getBoolean("USER_LOG_IN", false))
            assertEquals(42L, reopened.getLong("PENDING_REGISTRATION_EXPIRES_AT", 0L))

            val encrypted = directory.resolve("secure-store.bin").readBytes().decodeToString()
            assertFalse(encrypted.contains("secret-access-token"))

            Files.write(directory.resolve("secure-store.bin"), byteArrayOf(1, 2, 3))
            assertFails { reopened.getString("TOKEN") }
        } finally {
            directory.toFile().deleteRecursively()
        }
    }
}
