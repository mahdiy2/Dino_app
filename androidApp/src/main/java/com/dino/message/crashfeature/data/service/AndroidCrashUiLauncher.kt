package com.dino.message.crashfeature.data.service

import android.content.Context
import android.content.Intent
import com.dino.message.corefeature.domain.logging.AppLogger
import com.dino.message.crashfeature.presentation.activity.CrashReportActivity

class AndroidCrashUiLauncher(
    private val context: Context,
    private val appLogger: AppLogger,
) : CrashUiLauncher {
    override fun launchCrashReport(eventId: String) {
        val intent = Intent(context, CrashReportActivity::class.java).apply {
            putExtra(CrashReportActivity.EXTRA_EVENT_ID, eventId)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }

        runCatching {
            context.startActivity(intent)
        }.onFailure { error ->
            appLogger.error(TAG, "Unable to launch crash report activity", error)
        }
    }

    private companion object {
        const val TAG = "CrashUiLauncher"
    }
}
