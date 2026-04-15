package com.dino.message.activitynotificationfeature.data.worker

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.dino.message.BuildConfig
import com.dino.message.activitynotificationfeature.data.remote.ActivityNotificationRoute
import com.dino.message.activitynotificationfeature.data.remote.dto.GetActivityNotificationsResponse
import com.dino.message.chatfeature.data.notification.NotificationHelper
import com.dino.message.corefeature.data.background.NotificationDedupStore
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

class ActivityNotificationWorker(
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
            val route = ActivityNotificationRoute.GetNotifications(page = 1, limit = 10)
            val url = route.url + "&unread_only=true"
            val response: GetActivityNotificationsResponse = client.get(url) {
                contentType(ContentType.Application.Json)
                header("Authorization", "Bearer $token")
            }.body()

            if (response.total > 0 && response.notifications.isNotEmpty()) {
                showNotifications(response, dedupStore)
            }
            client.close()
            Result.success()
        } catch (e: Exception) {
            client.close()
            Result.retry()
        }
    }

    private fun showNotifications(
        response: GetActivityNotificationsResponse,
        dedupStore: NotificationDedupStore
    ) {
        val latestId = response.notifications.maxOfOrNull { it.id } ?: return
        if (!dedupStore.shouldNotifyActivity(latestId)) return

        val count = response.total
        val first = response.notifications.first()
        val actorName = first.actor.name ?: first.actor.username

        val body = if (count == 1) {
            buildActivityBody(first.kind, actorName, first)
        } else {
            "You have $count new activity notifications"
        }

        NotificationHelper.showActivityNotification(applicationContext, "Activity", body)
        dedupStore.markActivityNotified(latestId)
    }

    private fun buildActivityBody(
        kind: String,
        actorName: String,
        notification: com.dino.message.activitynotificationfeature.domain.model.ActivityNotification
    ): String {
        val extra = notification.extra
        return when (kind) {
            "follow" -> "$actorName started following you"
            "unfollow" -> "$actorName unfollowed you"
            "comment" -> {
                val preview = extra?.comment_preview ?: ""
                if (preview.isNotBlank()) "$actorName commented: $preview"
                else "$actorName commented on your post"
            }
            "comment_reply" -> {
                val preview = extra?.comment_preview ?: ""
                if (preview.isNotBlank()) "$actorName replied: $preview"
                else "$actorName replied to your comment"
            }
            "vote" -> {
                val label = extra?.vote_label ?: "voted"
                "$actorName ${label}d your post"
            }
            "post_like_milestone" -> {
                val count = extra?.engagement_count ?: extra?.milestone_count
                if (count != null) "Your post has received $count likes"
                else "Your post has received many likes"
            }
            "post_comment_milestone" -> {
                val count = extra?.engagement_count ?: extra?.milestone_count
                if (count != null) "Your post has received comments from $count people"
                else "Your post has received many comments"
            }
            else -> "$actorName interacted with your content"
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
