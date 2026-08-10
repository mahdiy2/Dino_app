package com.dino.message.corefeature.domain.usecase

import com.dino.message.chatfeature.data.notification.ActiveChatTracker
import com.dino.message.chatfeature.data.notification.ActiveGroupTracker
import com.dino.message.corefeature.data.db.MessageDatabase
import com.dino.message.corefeature.data.spref.SPrefManager
import com.dino.message.corefeature.domain.filesystem.FileSystemProvider
import com.dino.message.corefeature.domain.notification.AppNotificationController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ClearLocalSessionUseCase constructor(
    private val sPrefManager: SPrefManager,
    private val messageDatabase: MessageDatabase,
    private val appNotificationController: AppNotificationController,
    private val fileSystemProvider: FileSystemProvider,
) {
    suspend fun execute() {
        withContext(Dispatchers.IO) {
            runCatching { messageDatabase.clearAllTables() }
            runCatching { fileSystemProvider.clearAppCacheDirectories() }
        }

        runCatching { sPrefManager.clearAll() }
        runCatching { appNotificationController.clearAll() }
        runCatching { appNotificationController.clearPrivateMessageDedupe() }
        ActiveChatTracker.clearActiveChat()
        ActiveGroupTracker.clearActiveGroup()
        appNotificationController.disconnectInAppNotifications()
    }
}
