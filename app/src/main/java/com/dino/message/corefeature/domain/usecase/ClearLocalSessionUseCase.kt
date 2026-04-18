package com.dino.message.corefeature.domain.usecase

import android.content.Context
import com.dino.message.DinoOrderApplication
import com.dino.message.chatfeature.data.notification.ActiveChatTracker
import com.dino.message.chatfeature.data.notification.ActiveGroupTracker
import com.dino.message.chatfeature.data.notification.NotificationHelper
import com.dino.message.corefeature.data.db.MessageDatabase
import com.dino.message.corefeature.data.spref.SPrefManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ClearLocalSessionUseCase @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val sPrefManager: SPrefManager,
    private val messageDatabase: MessageDatabase
) {
    suspend fun execute() {
        val app = appContext.applicationContext as DinoOrderApplication

        withContext(Dispatchers.IO) {
            runCatching { messageDatabase.clearAllTables() }
            runCatching { clearDirectoryChildren(app.cacheDir) }
            runCatching { clearDirectoryChildren(app.externalCacheDir) }
            runCatching { clearDirectoryChildren(app.codeCacheDir) }
        }

        runCatching { sPrefManager.clearAll() }
        runCatching { NotificationHelper.clearAllNotifications(app) }
        ActiveChatTracker.clearActiveChat()
        ActiveGroupTracker.clearActiveGroup()
        app.inAppNotificationManager.disconnect()
    }

    private fun clearDirectoryChildren(directory: File?) {
        val files = directory?.listFiles() ?: return
        files.forEach { file ->
            file.deleteRecursively()
        }
    }
}
