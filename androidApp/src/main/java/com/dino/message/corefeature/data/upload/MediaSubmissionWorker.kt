package com.dino.message.corefeature.data.upload

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import org.koin.core.context.GlobalContext

class MediaSubmissionWorker(context: Context, parameters: WorkerParameters) :
    CoroutineWorker(context, parameters) {
    override suspend fun doWork(): Result {
        val id = inputData.getString("operation_id") ?: return Result.failure()
        return when (GlobalContext.get().get<AndroidMediaOutbox>().process(id, runAttemptCount)) {
            MediaWorkOutcome.Complete -> Result.success()
            MediaWorkOutcome.Retry -> Result.retry()
        }
    }
}
