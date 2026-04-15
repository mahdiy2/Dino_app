package com.dino.message.updatefeature.presentation.screen

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import com.dino.message.updatefeature.presentation.viewmodel.AppUpdateViewModel
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Destination<RootGraph>
@Composable
fun AppUpdateScreen(
    isForceUpdate: Boolean,
    title: String,
    message: String,
    downloadUrl: String,
) {
    val viewModel: AppUpdateViewModel = hiltViewModel()
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()

    var isDownloading by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf(message) }
    var progressPercent by remember { mutableIntStateOf(-1) }
    var pendingInstallPermissionFlow by remember { mutableStateOf(false) }

    BackHandler(enabled = isForceUpdate) {
        // Force-update gate: keep the user on this screen.
    }

    fun startDownloadAndInstall() {
        if (isDownloading) return

        scope.launch {
            isDownloading = true
            progressPercent = -1
            statusMessage = "Downloading update..."

            val result = downloadApkDirect(
                context = context,
                downloadUrl = downloadUrl,
                onProgress = { downloadedBytes, totalBytes ->
                    if (totalBytes > 0L) {
                        progressPercent = ((downloadedBytes * 100) / totalBytes)
                            .toInt()
                            .coerceIn(0, 100)
                        statusMessage = "Downloading update... $progressPercent%"
                    } else {
                        statusMessage = "Downloading update..."
                    }
                },
            )

            isDownloading = false
            if (result.file == null) {
                statusMessage = result.errorMessage ?: "Download failed. Please try again."
                return@launch
            }

            statusMessage = "Installing update..."
            val started = installApk(context, result.file)
            if (!started) {
                statusMessage = "Could not start installer. Please retry."
            }
        }
    }

    val installPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) {
        val canInstall = Build.VERSION.SDK_INT < Build.VERSION_CODES.O || context.packageManager.canRequestPackageInstalls()
        if (canInstall && pendingInstallPermissionFlow) {
            pendingInstallPermissionFlow = false
            startDownloadAndInstall()
        } else if (!canInstall) {
            pendingInstallPermissionFlow = false
            isDownloading = false
            statusMessage = "Install permission is required to continue."
        }
    }

    fun startUpdate() {
        val canInstall = Build.VERSION.SDK_INT < Build.VERSION_CODES.O || context.packageManager.canRequestPackageInstalls()
        if (!canInstall) {
            pendingInstallPermissionFlow = true
            val intent = Intent(
                Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                Uri.parse("package:${context.packageName}"),
            )
            installPermissionLauncher.launch(intent)
            return
        }

        startDownloadAndInstall()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = statusMessage,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(20.dp))

        if (isDownloading) {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(14.dp))
        }

        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = { startUpdate() },
            enabled = !isDownloading,
        ) {
            Text(if (isDownloading) "Downloading..." else "Update")
        }

        if (!isForceUpdate) {
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = { viewModel.continueWithoutUpdate() },
                enabled = !isDownloading,
            ) {
                Text("Later")
            }
        }
    }
}

private data class DownloadResult(
    val file: File? = null,
    val errorMessage: String? = null,
)

private suspend fun downloadApkDirect(
    context: Context,
    downloadUrl: String,
    onProgress: (downloadedBytes: Long, totalBytes: Long) -> Unit,
): DownloadResult = withContext(Dispatchers.IO) {
    val normalizedUrl = downloadUrl.trim()
    if (normalizedUrl.isBlank()) {
        return@withContext DownloadResult(errorMessage = "Update URL is missing.")
    }

    val targetDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
        ?: return@withContext DownloadResult(errorMessage = "Cannot access storage for update download.")

    val targetFile = File(targetDir, "sec_message_update.apk")
    if (targetFile.exists()) {
        targetFile.delete()
    }

    var connection: HttpURLConnection? = null
    try {
        connection = (URL(normalizedUrl).openConnection() as HttpURLConnection).apply {
            connectTimeout = 20_000
            readTimeout = 30_000
            instanceFollowRedirects = true
            requestMethod = "GET"
            setRequestProperty("User-Agent", "SecMessengerUpdater/1.0")
        }

        val statusCode = connection.responseCode
        if (statusCode !in 200..299) {
            return@withContext DownloadResult(
                errorMessage = "Download failed (HTTP $statusCode).",
            )
        }

        val totalBytes = connection.contentLengthLong
        var downloadedBytes = 0L

        BufferedInputStream(connection.inputStream).use { input ->
            FileOutputStream(targetFile).use { output ->
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                while (true) {
                    val read = input.read(buffer)
                    if (read < 0) break
                    output.write(buffer, 0, read)
                    downloadedBytes += read
                    withContext(Dispatchers.Main) {
                        onProgress(downloadedBytes, totalBytes)
                    }
                }
                output.flush()
            }
        }

        if (!targetFile.exists() || targetFile.length() <= 0L) {
            return@withContext DownloadResult(errorMessage = "Downloaded file is empty.")
        }

        DownloadResult(file = targetFile)
    } catch (e: Exception) {
        DownloadResult(errorMessage = e.message ?: "Download failed. Please try again.")
    } finally {
        connection?.disconnect()
    }
}

private fun installApk(context: Context, apkFile: File): Boolean {
    try {
        val apkUri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            apkFile,
        )

        val installIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(apkUri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        context.startActivity(installIntent)
        return true
    } catch (_: Exception) {
        return false
    }
}
