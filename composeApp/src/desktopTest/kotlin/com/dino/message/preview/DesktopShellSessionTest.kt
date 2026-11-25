package com.dino.message.preview

import com.dino.message.corefeature.data.remote.createAppHttpClient
import com.dino.message.corefeature.data.spref.DesktopEncryptedSecureKeyValueStore
import io.ktor.client.engine.cio.CIO
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlinx.coroutines.test.runTest

class DesktopShellSessionTest {
    @Test
    fun signsInRestoresFromDiskAndClearsOnLogout() = runTest {
        val backend = System.getenv("DINO_SESSION_TEST_URL") ?: return@runTest
        val username = checkNotNull(System.getenv("DINO_SESSION_TEST_USERNAME"))
        val password = checkNotNull(System.getenv("DINO_SESSION_TEST_PASSWORD"))
        val directory = Files.createTempDirectory("desktop-shell-session").toFile()
        val http = createAppHttpClient(CIO, backend.trimEnd('/') + "/")
        try {
            val session = DesktopShellSession(http, DesktopEncryptedSecureKeyValueStore(directory.toPath()))
            assertNull(session.restore())
            assertFailsWith<IllegalStateException> { session.signIn(username, "incorrect fixture password") }
            assertEquals(username, session.signIn(username, password))

            val reopened = DesktopShellSession(http, DesktopEncryptedSecureKeyValueStore(directory.toPath()))
            assertEquals(username, reopened.restore())
            reopened.signOut()
            assertNull(session.restore())
        } finally {
            http.close()
            directory.deleteRecursively()
        }
    }
}
