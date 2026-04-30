package com.dino.message.threadfeature.data.upload

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.dino.message.R
import com.dino.message.corefeature.data.remote.HaveRefreshTokenAPIImpl
import com.dino.message.corefeature.data.remote.util.appendTokenToHeader
import com.dino.message.corefeature.data.spref.SPrefManager
import com.dino.message.corefeature.util.RefreshTokenExpiredException
import com.dino.message.threadfeature.data.remote.ThreadRoute
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import io.ktor.client.HttpClient
import io.ktor.client.request.forms.InputProvider
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import io.ktor.utils.io.streams.asInput
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File
import java.io.InputStream
import java.util.UUID

class PostUploadWorker(
    appContext: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(appContext, workerParams) {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface WorkerEntryPoint {
        fun httpClient(): HttpClient
        fun sPrefManager(): SPrefManager
    }

    companion object {
        const val TAG_POST_UPLOAD = "post_upload_work"
        const val TAG_POST_UPLOAD_USER_PREFIX = "post_upload_user:"

        const val KEY_USERNAME = "upload_username"
        const val KEY_TEXT = "upload_text"
        const val KEY_FOLLOWERS_ONLY = "upload_followers_only"
        const val KEY_QUOTED_POST_ID = "upload_quoted_post_id"
        const val KEY_MEDIA_URIS = "upload_media_uris"
        const val KEY_MEDIA_NAMES = "upload_media_names"
        const val KEY_MEDIA_MIME_TYPES = "upload_media_mime_types"
        const val KEY_TRACK_TITLE = "upload_track_title"
        const val KEY_TRACK_ARTIST = "upload_track_artist"
        const val KEY_CREATED_AT = "upload_created_at"
        const val KEY_PROGRESS_PERCENT = "upload_progress_percent"
        const val KEY_ERROR_MESSAGE = "upload_error_message"

        const val ACTION_CANCEL_UPLOAD = "com.dino.message.threadfeature.upload.CANCEL_POST_UPLOAD"
        const val EXTRA_WORK_ID = "extra_work_id"

        private const val CHANNEL_ID = "post_uploads"
        private const val CHANNEL_NAME = "Post uploads"
        private const val NOTIFICATION_ID_BASE = 47000
    }

    private val notificationManager =
        applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    override suspend fun doWork(): Result {
        ensureUploadChannel()

        val username = inputData.getString(KEY_USERNAME).orEmpty()
        val text = inputData.getString(KEY_TEXT).orEmpty()
        val followersOnly = inputData.getBoolean(KEY_FOLLOWERS_ONLY, false)
        val quotedPostIdRaw = inputData.getLong(KEY_QUOTED_POST_ID, -1L)
        val quotedPostId = quotedPostIdRaw.takeIf { it > 0L }
        val mediaUris = inputData.getStringArray(KEY_MEDIA_URIS)?.toList().orEmpty()
        val mediaNames = inputData.getStringArray(KEY_MEDIA_NAMES)?.toList().orEmpty()
        val mediaMimeTypes = inputData.getStringArray(KEY_MEDIA_MIME_TYPES)?.toList().orEmpty()
        val trackTitle = inputData.getString(KEY_TRACK_TITLE)?.trim()?.takeIf { it.isNotBlank() }
        val trackArtist = inputData.getString(KEY_TRACK_ARTIST)?.trim()?.takeIf { it.isNotBlank() }

        if (username.isBlank() || text.isBlank()) {
            val message = "Invalid post upload payload"
            notifyFailure(message)
            return Result.failure(errorData(message))
        }
        if (mediaUris.size != mediaNames.size || mediaUris.size != mediaMimeTypes.size) {
            val message = "Invalid media metadata for upload"
            notifyFailure(message)
            return Result.failure(errorData(message))
        }

        try {
            setForeground(uploadForegroundInfo(progressPercent = 0))

            val entryPoint = EntryPointAccessors.fromApplication(
                applicationContext,
                WorkerEntryPoint::class.java
            )
            val httpClient = entryPoint.httpClient()
            val sPrefManager = entryPoint.sPrefManager()
            val refreshApi = HaveRefreshTokenAPIImpl(httpClient, sPrefManager)

            var token = sPrefManager.getToken().orEmpty().trim()
            if (token.isBlank()) {
                val message = "Session expired. Please log in again."
                notifyFailure(message)
                return Result.failure(errorData(message))
            }

            var response = uploadPost(
                httpClient = httpClient,
                token = token,
                text = text,
                followersOnly = followersOnly,
                quotedPostId = quotedPostId,
                mediaUris = mediaUris,
                mediaNames = mediaNames,
                mediaMimeTypes = mediaMimeTypes,
                trackTitle = trackTitle,
                trackArtist = trackArtist,
            )

            if (response.status == HttpStatusCode.Unauthorized) {
                token = try {
                    refreshApi.getToken().trim()
                } catch (_: RefreshTokenExpiredException) {
                    sPrefManager.clearAuthSession()
                    val message = "Session expired. Please log in again."
                    notifyFailure(message)
                    return Result.failure(errorData(message))
                }
                if (token.isBlank()) {
                    sPrefManager.clearAuthSession()
                    val message = "Session expired. Please log in again."
                    notifyFailure(message)
                    return Result.failure(errorData(message))
                }
                sPrefManager.setToken(token)

                response = uploadPost(
                    httpClient = httpClient,
                    token = token,
                    text = text,
                    followersOnly = followersOnly,
                    quotedPostId = quotedPostId,
                    mediaUris = mediaUris,
                    mediaNames = mediaNames,
                    mediaMimeTypes = mediaMimeTypes,
                    trackTitle = trackTitle,
                    trackArtist = trackArtist,
                )
            }

            if (response.status.isSuccess()) {
                setProgress(Data.Builder().putInt(KEY_PROGRESS_PERCENT, 100).build())
                notificationManager.cancel(notificationId())
                return Result.success()
            }

            val responseBody = runCatching { response.bodyAsText() }.getOrNull()
            val backendMessage = extractBackendError(responseBody)
            val message = backendMessage ?: "Upload failed with HTTP ${response.status.value}"
            notifyFailure(message)
            return Result.failure(
                errorData(message)
            )
        } catch (_: CancellationException) {
            notificationManager.cancel(notificationId())
            throw CancellationException()
        } catch (error: Exception) {
            val message = error.message ?: "Upload failed"
            notifyFailure(message)
            return Result.failure(errorData(message))
        }
    }

    private suspend fun uploadPost(
        httpClient: HttpClient,
        token: String,
        text: String,
        followersOnly: Boolean,
        quotedPostId: Long?,
        mediaUris: List<String>,
        mediaNames: List<String>,
        mediaMimeTypes: List<String>,
        trackTitle: String?,
        trackArtist: String?,
    ) = httpClient.post(ThreadRoute.CreatePost.url) {
        appendTokenToHeader(token)
        val knownTotalSize = mediaUris
            .map { uriString -> resolveFileSize(Uri.parse(uriString)) ?: 0L }
            .sum()
            .coerceAtLeast(1L)
        var uploadedBytes = 0L
        setBody(
            MultiPartFormDataContent(
                formData {
                    append("text", text)
                    append("followers_only", followersOnly.toString())
                    quotedPostId?.let { append("quoted_post_id", it.toString()) }
                    trackTitle?.let { append("track_title", it) }
                    trackArtist?.let { append("track_artist", it) }
                    mediaUris.forEachIndexed { index, uriString ->
                        val mimeType = mediaMimeTypes[index]
                        val fileName = mediaNames[index].ifBlank { "media_$index" }
                        val uri = Uri.parse(uriString)
                        append(
                            key = "media[]",
                            value = InputProvider(size = resolveFileSize(uri)) {
                                ProgressInputStream(openInputStream(uri)) { delta ->
                                    uploadedBytes += delta
                                    val progress = ((uploadedBytes * 100L) / knownTotalSize)
                                        .toInt()
                                        .coerceIn(0, 99)
                                    kotlinx.coroutines.runBlocking {
                                        setProgress(
                                            Data.Builder()
                                                .putInt(KEY_PROGRESS_PERCENT, progress)
                                                .build()
                                        )
                                        setForeground(uploadForegroundInfo(progress))
                                    }
                                }.asInput()
                            },
                            headers = Headers.build {
                                append(
                                    HttpHeaders.ContentDisposition,
                                    "form-data; name=\"media[]\"; filename=\"$fileName\""
                                )
                                append(HttpHeaders.ContentType, mimeType)
                            }
                        )
                    }
                }
            )
        )
    }

    private fun resolveFileSize(uri: Uri): Long? {
        if (uri.scheme == "file") {
            val path = uri.path ?: return null
            val file = File(path)
            return if (file.exists()) file.length() else null
        }
        val cursor = applicationContext.contentResolver.query(uri, null, null, null, null)
            ?: return null
        return cursor.use {
            if (!it.moveToFirst()) return@use null
            val sizeIndex = it.getColumnIndex(android.provider.OpenableColumns.SIZE)
            if (sizeIndex >= 0 && !it.isNull(sizeIndex)) it.getLong(sizeIndex) else null
        }
    }

    private fun openInputStream(uri: Uri): java.io.InputStream {
        if (uri.scheme == "file") {
            val path = uri.path ?: error("File path is missing")
            val file = File(path)
            if (!file.exists()) error("File does not exist")
            return file.inputStream()
        }
        return applicationContext.contentResolver.openInputStream(uri)
            ?: error("Unable to open input stream")
    }

    private fun uploadForegroundInfo(progressPercent: Int): ForegroundInfo {
        val notification = uploadNotification(progressPercent)
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(
                notificationId(),
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            ForegroundInfo(
                notificationId(),
                notification
            )
        }
    }

    private fun uploadNotification(progressPercent: Int): Notification {
        val cancelIntent = Intent(applicationContext, PostUploadCancelReceiver::class.java).apply {
            action = ACTION_CANCEL_UPLOAD
            putExtra(EXTRA_WORK_ID, id.toString())
        }
        val cancelPendingIntent = PendingIntent.getBroadcast(
            applicationContext,
            id.hashCode(),
            cancelIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(applicationContext.getString(R.string.uploading_post_title))
            .setContentText(applicationContext.getString(R.string.uploading_post_body))
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .setProgress(100, progressPercent.coerceIn(0, 100), false)
            .addAction(
                0,
                applicationContext.getString(R.string.cancel_upload),
                cancelPendingIntent
            )
            .build()
    }

    private fun ensureUploadChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_LOW
        )
        notificationManager.createNotificationChannel(channel)
    }

    private fun notifyFailure(message: String) {
        notificationManager.notify(
            notificationId(),
            NotificationCompat.Builder(applicationContext, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(applicationContext.getString(R.string.uploading_post_title))
                .setContentText(message)
                .setAutoCancel(true)
                .setOngoing(false)
                .build()
        )
    }

    private fun notificationId(): Int {
        return NOTIFICATION_ID_BASE + (id.hashCode() and 0x7FFF)
    }

    private fun errorData(message: String): Data {
        return Data.Builder()
            .putString(KEY_ERROR_MESSAGE, message)
            .build()
    }

    private fun extractBackendError(rawBody: String?): String? {
        val content = rawBody ?: return null
        return runCatching {
            Json.decodeFromString<JsonObject>(content)
                .get("error")
                ?.jsonPrimitive
                ?.content
                ?.trim()
                ?.takeIf { it.isNotBlank() }
        }.getOrNull()
    }
}

private class ProgressInputStream(
    private val delegate: InputStream,
    private val onBytesRead: (Long) -> Unit,
) : InputStream() {
    override fun read(): Int {
        val value = delegate.read()
        if (value >= 0) onBytesRead(1)
        return value
    }

    override fun read(b: ByteArray, off: Int, len: Int): Int {
        val count = delegate.read(b, off, len)
        if (count > 0) onBytesRead(count.toLong())
        return count
    }

    override fun close() {
        delegate.close()
    }
}
