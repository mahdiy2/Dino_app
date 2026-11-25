package com.dino.message.chatfeature.data.notification

import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.dino.message.corefeature.domain.notification.NotificationPayload
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NotificationPayloadTest {
    @Test
    fun payloadsPostOnExistingChannelsWithStableIdsAndTapActions() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val helper = NotificationHelper(context)
        val sender = "task14-smoke"
        val groupId = 1401
        val activityId = 1402L
        val privateId = sender.hashCode()
        val groupNotificationId = "group_$groupId".hashCode()
        try {
            helper.ensureAppChannels()
            helper.show(NotificationPayload.PrivateMessage(sender, "Private message"))
            helper.show(NotificationPayload.GroupMessage(groupId, "Smoke group", "Group message"))
            helper.show(NotificationPayload.Activity("Activity", "Activity update", activityId))

            val expected = mapOf(
                privateId to ("messages" to "Private message"),
                groupNotificationId to ("messages" to "Group message"),
                activityId.toInt() to ("activity_notifications" to "Activity update"),
            )
            val posted = manager.activeNotifications.associateBy { it.id }
            for ((id, channelAndBody) in expected) {
                val (channel, body) = channelAndBody
                val notification = requireNotNull(posted[id]) { "Missing notification $id" }.notification
                assertEquals(channel, notification.channelId)
                assertEquals(body, notification.extras.getCharSequence(Notification.EXTRA_TEXT).toString())
                assertNotNull(notification.contentIntent)
            }
        } finally {
            helper.clearPrivateChat(sender)
            helper.clearGroupChat(groupId)
            manager.cancel(activityId.toInt())
        }
    }
}
