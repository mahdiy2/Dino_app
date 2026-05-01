package com.dino.message.threadfeature.data.upload

import android.content.Context
import android.content.SharedPreferences
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PostUploadManager @Inject constructor(
    @ApplicationContext private val appContext: Context,
) {
    companion object {
        private const val UNIQUE_WORK_NAME_PREFIX = "post_upload_user_unique:"
    }

    private val workManager: WorkManager = WorkManager.getInstance(appContext)
    private val metadataStore: SharedPreferences = appContext.getSharedPreferences(
        "post_upload_metadata",
        Context.MODE_PRIVATE
    )

    fun enqueueUpload(
        username: String,
        text: String,
        followersOnly: Boolean,
        quotedPostId: Long?,
        mediaUris: List<String>,
        mediaNames: List<String>,
        mediaMimeTypes: List<String>,
        trackTitle: String? = null,
        trackArtist: String? = null,
    ): UUID? {
        if (hasInFlightUpload(username)) {
            return null
        }

        val request = OneTimeWorkRequestBuilder<PostUploadWorker>()
            .setInputData(
                Data.Builder()
                    .putString(PostUploadWorker.KEY_USERNAME, username)
                    .putString(PostUploadWorker.KEY_TEXT, text)
                    .putBoolean(PostUploadWorker.KEY_FOLLOWERS_ONLY, followersOnly)
                    .putLong(PostUploadWorker.KEY_QUOTED_POST_ID, quotedPostId ?: -1L)
                    .putStringArray(PostUploadWorker.KEY_MEDIA_URIS, mediaUris.toTypedArray())
                    .putStringArray(PostUploadWorker.KEY_MEDIA_NAMES, mediaNames.toTypedArray())
                    .putStringArray(PostUploadWorker.KEY_MEDIA_MIME_TYPES, mediaMimeTypes.toTypedArray())
                    .putString(PostUploadWorker.KEY_TRACK_TITLE, trackTitle)
                    .putString(PostUploadWorker.KEY_TRACK_ARTIST, trackArtist)
                    .putLong(PostUploadWorker.KEY_CREATED_AT, System.currentTimeMillis())
                    .build()
            )
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .addTag(PostUploadWorker.TAG_POST_UPLOAD)
            .addTag("${PostUploadWorker.TAG_POST_UPLOAD_USER_PREFIX}$username")
            .build()

        val uniqueWorkName = "$UNIQUE_WORK_NAME_PREFIX$username"
        workManager.enqueueUniqueWork(
            uniqueWorkName,
            ExistingWorkPolicy.KEEP,
            request
        )
        saveMetadata(
            workId = request.id,
            text = text,
            mediaCount = mediaUris.size,
            createdAtMs = System.currentTimeMillis()
        )
        return request.id
    }

    fun hasInFlightUpload(username: String): Boolean {
        val userTag = "${PostUploadWorker.TAG_POST_UPLOAD_USER_PREFIX}$username"
        val workInfos = runCatching {
            workManager.getWorkInfosByTag(userTag).get()
        }.getOrNull() ?: return false
        return workInfos.any { info ->
            info.state == WorkInfo.State.ENQUEUED ||
                info.state == WorkInfo.State.RUNNING ||
                info.state == WorkInfo.State.BLOCKED
        }
    }

    fun observePendingUploads(username: String): Flow<List<PendingPostUpload>> {
        val userTag = "${PostUploadWorker.TAG_POST_UPLOAD_USER_PREFIX}$username"
        return workManager.getWorkInfosByTagFlow(PostUploadWorker.TAG_POST_UPLOAD)
            .map { workInfos ->
                workInfos
                    .asSequence()
                    .filter { it.tags.contains(userTag) }
                    .filter { info ->
                        info.state == WorkInfo.State.ENQUEUED ||
                            info.state == WorkInfo.State.RUNNING ||
                            info.state == WorkInfo.State.BLOCKED
                    }
                    .map { info ->
                        val metadata = readMetadata(info.id)
                        PendingPostUpload(
                            workId = info.id,
                            text = metadata?.text.orEmpty(),
                            mediaCount = metadata?.mediaCount ?: 0,
                            progressPercent = info.progress.getInt(
                                PostUploadWorker.KEY_PROGRESS_PERCENT,
                                0
                            ),
                            isRunning = info.state == WorkInfo.State.RUNNING,
                            createdAtEpochMs = metadata?.createdAtMs ?: 0L,
                        )
                    }
                    .sortedByDescending { it.createdAtEpochMs }
                    .toList()
            }
    }

    fun observeSuccessfulUploadIds(username: String): Flow<Set<UUID>> {
        val userTag = "${PostUploadWorker.TAG_POST_UPLOAD_USER_PREFIX}$username"
        return workManager.getWorkInfosByTagFlow(PostUploadWorker.TAG_POST_UPLOAD)
            .map { workInfos ->
                workInfos
                    .asSequence()
                    .filter { it.tags.contains(userTag) }
                    .onEach { info ->
                        if (info.state.isFinished) {
                            removeMetadata(info.id)
                        }
                    }
                    .filter { it.state == WorkInfo.State.SUCCEEDED }
                    .map { it.id }
                    .toSet()
            }
    }

    fun cancelUpload(workId: UUID) {
        workManager.cancelWorkById(workId)
    }

    private fun saveMetadata(
        workId: UUID,
        text: String,
        mediaCount: Int,
        createdAtMs: Long,
    ) {
        metadataStore.edit()
            .putString(keyFor(workId, "text"), text)
            .putInt(keyFor(workId, "mediaCount"), mediaCount)
            .putLong(keyFor(workId, "createdAt"), createdAtMs)
            .apply()
    }

    private fun readMetadata(workId: UUID): UploadMetadata? {
        val textKey = keyFor(workId, "text")
        if (!metadataStore.contains(textKey)) return null
        return UploadMetadata(
            text = metadataStore.getString(textKey, "").orEmpty(),
            mediaCount = metadataStore.getInt(keyFor(workId, "mediaCount"), 0),
            createdAtMs = metadataStore.getLong(keyFor(workId, "createdAt"), 0L),
        )
    }

    private fun removeMetadata(workId: UUID) {
        metadataStore.edit()
            .remove(keyFor(workId, "text"))
            .remove(keyFor(workId, "mediaCount"))
            .remove(keyFor(workId, "createdAt"))
            .apply()
    }

    private fun keyFor(workId: UUID, suffix: String): String = "${workId}_$suffix"

    private data class UploadMetadata(
        val text: String,
        val mediaCount: Int,
        val createdAtMs: Long,
    )
}
