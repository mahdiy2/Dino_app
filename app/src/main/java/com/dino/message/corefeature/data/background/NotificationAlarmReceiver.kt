package com.dino.message.corefeature.data.background

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class NotificationAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        Log.d(TAG, "Alarm triggered, syncing polling workers")
        NotificationPollingOrchestrator.from(context).onAlarmTriggered()
    }

    companion object {
        private const val TAG = "NotificationAlarmReceiver"
    }
}
