package com.dino.message.chatfeature.data.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.dino.message.R
import com.dino.message.chatfeature.data.remote.ChatRoute
import com.dino.message.chatfeature.data.remote.dto.UnreadResponse
import com.dino.message.corefeature.presentation.activity.MainActivity
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
                showNotifications(response)
            }
            client.close()
            Result.success()
        } catch (e: Exception) {
            client.close()
            Result.retry()
        }
    }

    private fun showNotifications(response: UnreadResponse) {
        val channelId = "messages"
        val notificationManager =
            applicationContext.getSystemService(Context.NOTIFICATION_SERVICE)
                as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId, "Messages",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        for (sender in response.senders) {
            val body = when (sender.lastType) {
                "image" -> "Sent you an image"
                "video" -> "Sent you a video"
                "voice" -> "Sent you a voice message"
                "file" -> "Sent you a file"
                "mixed" -> "Sent you a message with attachment"
                else -> if (sender.count > 1) "${sender.count} new messages"
                else "Sent you a message"
            }

            val intent = Intent(applicationContext, MainActivity::class.java).apply {
                putExtra("username", sender.sender)
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                applicationContext,
                sender.sender.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(applicationContext, channelId)
                .setSmallIcon(R.drawable.ic_message_filled)
                .setContentTitle(sender.sender)
                .setContentText(body)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .build()

            notificationManager.notify(sender.sender.hashCode(), notification)
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
