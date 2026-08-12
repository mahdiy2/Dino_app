package com.dino.message.corefeature.data.notification

import com.dino.message.corefeature.data.spref.SPrefManager
import com.dino.message.corefeature.domain.notification.NotificationTokenProvider

class SPrefNotificationTokenProvider(
    private val sPrefManager: SPrefManager,
) : NotificationTokenProvider {
    override fun notificationSocketToken(): String {
        return sPrefManager.getToken()
    }
}
