package com.dino.message.corefeature.data.background

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import dagger.hilt.android.EntryPointAccessors

class NotificationBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        when (intent?.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_LOCKED_BOOT_COMPLETED -> {
                Log.d(TAG, "Received ${intent.action}, restoring notification polling")
                val entryPoint = EntryPointAccessors.fromApplication(
                    context.applicationContext,
                    NotificationStartupEntryPoint::class.java
                )
                entryPoint.notificationStartupCoordinator().sync()
            }
        }
    }

    companion object {
        private const val TAG = "NotificationBootReceiver"
    }
}
