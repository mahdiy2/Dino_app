package com.dino.message.activitynotificationfeature.data.worker

import android.content.Context
import com.dino.message.corefeature.data.background.NotificationPollingOrchestrator

object ActivityPollingScheduler {

    fun schedule(context: Context) {
        NotificationPollingOrchestrator.from(context).ensureScheduled()
    }

    fun cancel(context: Context) {
        NotificationPollingOrchestrator.from(context).cancelAll()
    }
}
