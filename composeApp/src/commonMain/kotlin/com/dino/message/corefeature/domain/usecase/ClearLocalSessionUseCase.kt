package com.dino.message.corefeature.domain.usecase

import com.dino.message.corefeature.domain.coroutine.AppDispatchers
import com.dino.message.corefeature.domain.filesystem.FileSystemProvider
import com.dino.message.corefeature.domain.notification.AppNotificationController
import com.dino.message.corefeature.domain.session.ActiveConversationStateCleaner
import com.dino.message.corefeature.domain.session.LocalMessageStoreCleaner
import com.dino.message.corefeature.domain.session.LocalSessionCleaner
import kotlinx.coroutines.withContext

class ClearLocalSessionUseCase constructor(
    private val localSessionCleaner: LocalSessionCleaner,
    private val localMessageStoreCleaner: LocalMessageStoreCleaner,
    private val activeConversationStateCleaner: ActiveConversationStateCleaner,
    private val appNotificationController: AppNotificationController,
    private val fileSystemProvider: FileSystemProvider,
    private val dispatchers: AppDispatchers,
) {
    suspend fun execute() {
        withContext(dispatchers.io) {
            runCatching { localMessageStoreCleaner.clearAllTables() }
            runCatching { fileSystemProvider.clearAppCacheDirectories() }
        }

        runCatching { localSessionCleaner.clearAll() }
        runCatching { appNotificationController.clearAll() }
        runCatching { appNotificationController.clearPrivateMessageDedupe() }
        activeConversationStateCleaner.clearActiveConversations()
        appNotificationController.disconnectInAppNotifications()
    }
}
