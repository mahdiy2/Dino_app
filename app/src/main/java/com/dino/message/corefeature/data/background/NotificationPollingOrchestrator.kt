package com.dino.message.corefeature.data.background

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.dino.message.activitynotificationfeature.data.worker.ActivityNotificationWorker
import com.dino.message.chatfeature.data.worker.UnreadMessagesWorker
import com.dino.message.groupfeature.data.worker.GroupUnreadMessagesWorker
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationPollingOrchestrator @Inject constructor(
    @ApplicationContext private val context: Context
) {

    fun ensureScheduled() {
        scheduleNextAlarm()
    }

    fun onAlarmTriggered() {
        scheduleNextAlarm()
        enqueueWorkers()
    }

    fun cancelAll() {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pendingIntent = createPendingIntent()
        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()

        WorkManager.getInstance(context).cancelUniqueWork(UNREAD_WORK_NAME)
        WorkManager.getInstance(context).cancelUniqueWork(ACTIVITY_WORK_NAME)
        WorkManager.getInstance(context).cancelUniqueWork(GROUP_WORK_NAME)
        Log.d(TAG, "Cancelled polling alarm and workers")
    }

    private fun enqueueWorkers() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val unreadWorkRequest = OneTimeWorkRequestBuilder<UnreadMessagesWorker>()
            .setConstraints(constraints)
            .build()

        val activityWorkRequest = OneTimeWorkRequestBuilder<ActivityNotificationWorker>()
            .setConstraints(constraints)
            .build()

        val groupWorkRequest = OneTimeWorkRequestBuilder<GroupUnreadMessagesWorker>()
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            UNREAD_WORK_NAME,
            ExistingWorkPolicy.KEEP,
            unreadWorkRequest
        )
        WorkManager.getInstance(context).enqueueUniqueWork(
            ACTIVITY_WORK_NAME,
            ExistingWorkPolicy.KEEP,
            activityWorkRequest
        )
        WorkManager.getInstance(context).enqueueUniqueWork(
            GROUP_WORK_NAME,
            ExistingWorkPolicy.KEEP,
            groupWorkRequest
        )
        Log.d(TAG, "Enqueued unread + activity + group polling workers")
    }

    private fun scheduleNextAlarm() {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pendingIntent = createPendingIntent()
        val triggerAtMillis = System.currentTimeMillis() +
            NotificationPollingConfig.POLLING_INTERVAL_MINUTES * 60 * 1000L

        try {
            alarmManager.cancel(pendingIntent)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                !alarmManager.canScheduleExactAlarms()
            ) {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
                Log.d(TAG, "Scheduled inexact idle alarm")
                return
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            } else {
                alarmManager.set(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            }

            Log.d(
                TAG,
                "Scheduled alarm in ${NotificationPollingConfig.POLLING_INTERVAL_MINUTES} minutes"
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to schedule alarm", e)
        }
    }

    private fun createPendingIntent(): PendingIntent {
        val intent = Intent(context, NotificationAlarmReceiver::class.java)
        return PendingIntent.getBroadcast(
            context,
            ALARM_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    companion object {
        private const val TAG = "NotificationPolling"
        private const val ALARM_REQUEST_CODE = 1001
        private const val UNREAD_WORK_NAME = "unread_alarm_worker"
        private const val ACTIVITY_WORK_NAME = "activity_alarm_worker"
        private const val GROUP_WORK_NAME = "group_alarm_worker"

        fun from(context: Context): NotificationPollingOrchestrator {
            val appContext = context.applicationContext
            val entryPoint = EntryPointAccessors.fromApplication(
                appContext,
                NotificationPollingEntryPoint::class.java
            )
            return entryPoint.notificationPollingOrchestrator()
        }
    }
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface NotificationPollingEntryPoint {
    fun notificationPollingOrchestrator(): NotificationPollingOrchestrator
}
