package com.dino.message.threadfeature.data.upload

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.WorkManager
import java.util.UUID

class PostUploadCancelReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != PostUploadWorker.ACTION_CANCEL_UPLOAD) return
        val rawWorkId = intent.getStringExtra(PostUploadWorker.EXTRA_WORK_ID) ?: return
        val workId = runCatching { UUID.fromString(rawWorkId) }.getOrNull() ?: return
        WorkManager.getInstance(context).cancelWorkById(workId)
    }
}
