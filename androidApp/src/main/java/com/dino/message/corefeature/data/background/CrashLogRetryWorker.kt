package com.dino.message.corefeature.data.background

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.dino.message.crashfeature.data.service.CrashReporter
import kotlinx.coroutines.CancellationException
import org.koin.core.context.GlobalContext

class CrashLogRetryWorker(context: Context, parameters: WorkerParameters) :
    CoroutineWorker(context, parameters) {
    override suspend fun doWork(): Result {
        val flushed = try {
            GlobalContext.get().get<CrashReporter>().retryPendingLogs()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            false
        }
        return if (flushed) Result.success() else Result.retry()
    }
}
