package com.dino.message.corefeature.data.upload

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import java.util.concurrent.TimeUnit
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

interface MediaSubmissionWorkScheduler {
    fun schedule(id: String, replace: Boolean)
    fun cancel(id: String)
}

class WorkManagerMediaSubmissionScheduler(context: Context) : MediaSubmissionWorkScheduler {
    private val work = WorkManager.getInstance(context)

    override fun schedule(id: String, replace: Boolean) {
        val request = OneTimeWorkRequestBuilder<MediaSubmissionWorker>()
            .setInputData(workDataOf("operation_id" to id))
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()
        work.enqueueUniqueWork(
            "media-submission:$id",
            if (replace) ExistingWorkPolicy.REPLACE else ExistingWorkPolicy.KEEP,
            request,
        )
    }

    override fun cancel(id: String) {
        work.cancelUniqueWork("media-submission:$id")
    }
}

fun interface UploadedAttachmentTransport {
    fun enqueue(owner: String, sessionId: String, event: String, payload: String, dedupeKey: String): Boolean
}

/** Null means the server could not be reached or returned a transient error. */
suspend fun HttpClient.mediaReplayCapability(): Boolean? = try {
    val response = get("api/messages/media-submission-capabilities")
    when (response.status.value) {
        200 -> Json.parseToJsonElement(response.bodyAsText()).jsonObject["replay_version"]
            ?.jsonPrimitive?.intOrNull == 1
        404, 405 -> false
        else -> null
    }
} catch (cancelled: CancellationException) {
    throw cancelled
} catch (_: Exception) {
    null
}

