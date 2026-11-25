package com.dino.message.preview

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.dino.message.corefeature.data.remote.createAppHttpClient
import com.dino.message.corefeature.data.spref.DesktopEncryptedSecureKeyValueStore
import com.dino.message.corefeature.presentation.navigation.AuthenticatedApp
import io.ktor.client.engine.cio.CIO
import java.nio.file.Path

fun main() {
    val baseUrl = System.getenv("APP_BASE_URL")?.takeIf { it.isNotBlank() }
        ?: "http://127.0.0.1:5000/"
    val http = createAppHttpClient(CIO, baseUrl.trimEnd('/') + "/")
    val dataDirectory = System.getenv("XDG_DATA_HOME")?.takeIf { it.isNotBlank() }
        ?: Path.of(System.getProperty("user.home"), ".local", "share").toString()
    val store = DesktopEncryptedSecureKeyValueStore(Path.of(dataDirectory, "secmessage", "session"))
    val session = DesktopShellSession(http, store)
    try {
        application {
            Window(onCloseRequest = ::exitApplication, title = "SecMessage") {
                AuthenticatedApp(session)
            }
        }
    } finally {
        http.close()
    }
}
