package com.dino.message.corefeature.data.background

import android.util.Log
import com.dino.message.corefeature.domain.session.SessionProvider
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationStartupCoordinator @Inject constructor(
    private val sessionProvider: SessionProvider,
    private val pollingOrchestrator: NotificationPollingOrchestrator
) {

    fun sync() {
        if (!sessionProvider.hasActiveSession()) {
            pollingOrchestrator.cancelAll()
            Log.d(TAG, "No active session, disabled notification polling")
            return
        }

        pollingOrchestrator.ensureScheduled()
        Log.d(TAG, "Active session found, ensured notification polling is scheduled")
    }

    companion object {
        private const val TAG = "NotificationStartup"
    }
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface NotificationStartupEntryPoint {
    fun notificationStartupCoordinator(): NotificationStartupCoordinator
}
