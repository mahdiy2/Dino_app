package com.dino.message.preview

import com.dino.message.corefeature.data.remote.createAppHttpClient
import com.dino.message.corefeature.data.crypto.createPlatformMessageCrypto
import com.dino.message.corefeature.presentation.navigation.AccountKeyWorkflow
import com.dino.message.corefeature.presentation.navigation.AccountKeyApi
import com.dino.message.corefeature.presentation.navigation.ShellRegistrationApi
import com.dino.message.corefeature.data.spref.DesktopEncryptedSecureKeyValueStore
import io.ktor.client.engine.cio.CIO
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.async

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

    @Test
    fun registersImportsAndReplacesKeysAgainstBackend() = runTest {
        val backend = System.getenv("DINO_SESSION_TEST_URL") ?: return@runTest
        val username = "task2-${java.util.UUID.randomUUID().toString().take(12)}"
        val password = "Disposable123!"
        val directory = Files.createTempDirectory("desktop-key-workflow").toFile()
        val http = createAppHttpClient(CIO, backend.trimEnd('/') + "/")
        try {
            val store = DesktopEncryptedSecureKeyValueStore(directory.toPath())
            val session = DesktopShellSession(http, store)
            val crypto = createPlatformMessageCrypto()
            val vault = DesktopAccountKeyVault(store)
            val registration = ShellRegistrationApi(http)
            val workflow = AccountKeyWorkflow(vault, session, registration, crypto, needsPassphrase = false)
            val staged = workflow.beginRegistration(username, password, "", null)
            val reopened = AccountKeyWorkflow(vault, session, registration, crypto, needsPassphrase = false)
            assertEquals(staged, reopened.beginRegistration(username, password, "", null))
            assertTrue(reopened.confirmRegistration(username, "") in 200..299)
            assertEquals(username, session.signIn(username, password))
            assertEquals(staged.privateKey, reopened.open("")?.privateKey)

            assertFailsWith<IllegalStateException> { reopened.importBackup("", "invalid backup") }
            val replacement = reopened.prepareReset("")
            assertNotEquals(staged.privateKey, replacement.privateKey)
            assertFailsWith<IllegalStateException> { reopened.replaceKey("", "incorrect password") }
            assertEquals(staged.privateKey, reopened.open("")?.privateKey)
            assertEquals(replacement.privateKey, reopened.replaceKey("", password).privateKey)
            assertEquals(replacement.privateKey, reopened.importBackup("", replacement.privateKey).privateKey)

            val lostResponse = object : AccountKeyApi by session {
                override suspend fun rotate(password: String, publicKey: String) {
                    session.rotate(password, publicKey)
                    error("Simulated lost rotation response")
                }
            }
            val interrupted = AccountKeyWorkflow(vault, lostResponse, registration, crypto, needsPassphrase = false)
            val pending = interrupted.prepareReset("")
            assertFailsWith<IllegalStateException> { interrupted.replaceKey("", password) }
            assertEquals(pending.privateKey, reopened.open("")?.privateKey)
            session.signOut()
            assertNull(session.restore())

            val other = checkNotNull(System.getenv("DINO_SESSION_TEST_USERNAME"))
            assertEquals(other, session.signIn(other, checkNotNull(System.getenv("DINO_SESSION_TEST_PASSWORD"))))
            assertNull(reopened.open(""))
        } finally {
            http.close()
            directory.deleteRecursively()
        }
    }


    @Test
    fun concurrentExpiredRequestsShareOneRefresh() = runTest {
        val directory = Files.createTempDirectory("desktop-session-refresh").toFile()
        val refreshes = java.util.concurrent.atomic.AtomicInteger()
        val server = com.sun.net.httpserver.HttpServer.create(java.net.InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/api/auth") { exchange ->
            val (status, body) = when (exchange.requestURI.path) {
                "/api/auth/login" -> 200 to """{"access_token":"expired","refresh_token":"saved"}"""
                "/api/auth/refresh" -> {
                    refreshes.incrementAndGet()
                    200 to """{"access_token":"fresh"}"""
                }
                else -> if (exchange.requestHeaders.getFirst("Authorization") == "Bearer fresh") {
                    200 to """{"username":"alice","public_key":null}"""
                } else {
                    401 to "{}"
                }
            }
            val payload = body.encodeToByteArray()
            exchange.responseHeaders.add("Content-Type", "application/json")
            exchange.sendResponseHeaders(status, payload.size.toLong())
            exchange.responseBody.use { it.write(payload) }
        }
        server.start()
        val http = createAppHttpClient(CIO, "http://127.0.0.1:${server.address.port}/")
        try {
            val session = DesktopShellSession(http, DesktopEncryptedSecureKeyValueStore(directory.toPath()))
            session.signIn("alice", "fixture")
            kotlinx.coroutines.coroutineScope {
                List(8) { async { session.currentKey().username } }.forEach {
                    assertEquals("alice", it.await())
                }
            }
            assertEquals(1, refreshes.get())
        } finally {
            http.close()
            server.stop(0)
            directory.deleteRecursively()
        }
    }

}
