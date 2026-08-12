package com.dino.message.corefeature.domain.notification

interface NotificationTokenProvider {
    fun notificationSocketToken(): String
}

interface NotificationSocketConnection {
    fun connect(token: String)
    fun disconnect()
}

interface NotificationSocketController {
    fun connectIfAuthenticated()
    fun disconnect()
}

class DefaultNotificationSocketController(
    private val tokenProvider: NotificationTokenProvider,
    private val socketConnection: NotificationSocketConnection,
) : NotificationSocketController {
    override fun connectIfAuthenticated() {
        val token = tokenProvider.notificationSocketToken()
        if (token.isNotBlank()) {
            socketConnection.connect(token)
        }
    }

    override fun disconnect() {
        socketConnection.disconnect()
    }
}
