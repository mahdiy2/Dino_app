package com.dino.message.chatfeature.data.worker

import android.content.Context
import com.dino.message.corefeature.data.background.NotificationPollingOrchestrator

object UnreadPollingScheduler {

    fun schedule(context: Context) {
        NotificationPollingOrchestrator.from(context).ensureScheduled()
    }

    fun cancel(context: Context) {
        NotificationPollingOrchestrator.from(context).cancelAll()
    }
}
