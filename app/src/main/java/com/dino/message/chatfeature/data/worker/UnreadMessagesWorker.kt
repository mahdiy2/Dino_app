package com.dino.message.chatfeature.data.worker

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.dino.message.chatfeature.data.remote.ChatRoute
import com.dino.message.chatfeature.data.remote.dto.UnreadResponse
import com.dino.message.chatfeature.data.notification.ActiveChatTracker
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
import com.dino.message.BuildConfig

class UnreadMessagesWorker(
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
            val response: UnreadResponse = client.get(ChatRoute.GetUnread.url) {
                contentType(ContentType.Application.Json)
                header("Authorization", "Bearer $token")
            }.body()

            if (response.total > 0) {
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
        response: UnreadResponse,
        dedupStore: NotificationDedupStore
    ) {
        for (sender in response.senders) {
            if (ActiveChatTracker.isChattingWith(sender.sender)) continue
            if (!dedupStore.shouldNotifyMessage(sender.sender, sender.lastTimestamp)) continue

            val body = when (sender.lastType) {
                "image" -> "Sent you an image"
                "video" -> "Sent you a video"
                "voice" -> "Sent you a voice message"
                "file" -> "Sent you a file"
                "mixed" -> "Sent you a message with attachment"
                else -> if (sender.count > 1) "${sender.count} new messages"
                else "Sent you a message"
            }

            NotificationHelper.showMessageNotification(applicationContext, sender.sender, body)
            dedupStore.markMessageNotified(sender.sender, sender.lastTimestamp)
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
