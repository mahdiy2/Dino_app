package com.dino.message.corefeature.domain.notification

import org.junit.Assert.assertEquals
import org.junit.Test

class DefaultNotificationSocketControllerTest {

    @Test
    fun connectIfAuthenticated_withBlankToken_doesNotConnect() {
        val connection = RecordingNotificationSocketConnection()
        val controller = DefaultNotificationSocketController(
            tokenProvider = StaticNotificationTokenProvider(" "),
            socketConnection = connection,
        )

        controller.connectIfAuthenticated()

        assertEquals(emptyList<String>(), connection.connectedTokens)
    }

    @Test
    fun connectIfAuthenticated_withToken_connectsWithToken() {
        val connection = RecordingNotificationSocketConnection()
        val controller = DefaultNotificationSocketController(
            tokenProvider = StaticNotificationTokenProvider("Bearer token"),
            socketConnection = connection,
        )

        controller.connectIfAuthenticated()

        assertEquals(listOf("Bearer token"), connection.connectedTokens)
    }

    @Test
    fun disconnect_delegatesToConnection() {
        val connection = RecordingNotificationSocketConnection()
        val controller = DefaultNotificationSocketController(
            tokenProvider = StaticNotificationTokenProvider("Bearer token"),
            socketConnection = connection,
        )

        controller.disconnect()

        assertEquals(1, connection.disconnectCount)
    }

    private class StaticNotificationTokenProvider(
        private val token: String,
    ) : NotificationTokenProvider {
        override fun notificationSocketToken(): String = token
    }

    private class RecordingNotificationSocketConnection : NotificationSocketConnection {
        val connectedTokens = mutableListOf<String>()
        var disconnectCount = 0

        override fun connect(token: String) {
            connectedTokens += token
        }

        override fun disconnect() {
            disconnectCount += 1
        }
    }
}
