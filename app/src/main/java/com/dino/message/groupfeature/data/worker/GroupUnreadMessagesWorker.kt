package com.dino.message.groupfeature.data.worker

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.dino.message.BuildConfig
import com.dino.message.chatfeature.data.notification.ActiveGroupTracker
import com.dino.message.chatfeature.data.notification.NotificationHelper
import com.dino.message.corefeature.data.background.NotificationDedupStore
import com.dino.message.groupfeature.data.remote.GroupRoute
import com.dino.message.groupfeature.data.remote.dto.GroupUnreadSummaryResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

class GroupUnreadMessagesWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val token = getJwtToken() ?: return Result.success()
        val dedupStore = NotificationDedupStore(applicationContext)

        val client = HttpClient(CIO) {
            defaultRequest { url(BuildConfig.BASE_URL) }
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }

        return try {
            val response: GroupUnreadSummaryResponse = client.get(GroupRoute.GetUnreadGroups.url) {
                contentType(ContentType.Application.Json)
                header("Authorization", "Bearer $token")
            }.body()

            if (response.total > 0) {
                showGroupNotifications(response, dedupStore)
            }

            client.close()
            Result.success()
        } catch (e: Exception) {
            client.close()
            Result.retry()
        }
    }

    private fun showGroupNotifications(
        response: GroupUnreadSummaryResponse,
        dedupStore: NotificationDedupStore
    ) {
        for (group in response.groups) {
            if (ActiveGroupTracker.isViewingGroup(group.groupId)) continue
            if (!dedupStore.shouldNotifyGroup(group.groupId, group.lastTimestamp)) continue

            val body = when (group.lastType) {
                "image" -> "${group.lastSender} sent an image"
                "video" -> "${group.lastSender} sent a video"
                "voice" -> "${group.lastSender} sent a voice message"
                "file" -> "${group.lastSender} sent a file"
                "mixed" -> "${group.lastSender} sent a message with attachment"
                else -> if (group.count > 1) "${group.count} new messages"
                else "${group.lastSender} sent a message"
            }

            NotificationHelper.showGroupMessageNotification(
                context = applicationContext,
                groupId = group.groupId,
                groupName = group.groupName,
                body = body
            )
            dedupStore.markGroupNotified(group.groupId, group.lastTimestamp)
        }
    }

    private fun getJwtToken(): String? {
        return try {
            val masterKey = MasterKey.Builder(applicationContext)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            val prefs = EncryptedSharedPreferences.create(
                applicationContext,
                "sec_message",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
            prefs.getString("TOKEN", null)
        } catch (e: Exception) {
            null
        }
    }
}
