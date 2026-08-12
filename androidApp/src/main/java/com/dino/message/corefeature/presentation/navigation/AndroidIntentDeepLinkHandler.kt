package com.dino.message.corefeature.presentation.navigation

import android.content.Intent

class AndroidIntentDeepLinkHandler : AppDeepLinkHandler<Intent> {
    override fun consume(input: Intent?): AppDeepLinkCommand? {
        val intent = input ?: return null

        parsePrivateChat(intent)?.let { return it }
        parseGroupChat(intent)?.let { return it }
        parseActivityNotifications(intent)?.let { return it }

        return null
    }

    private fun parsePrivateChat(intent: Intent): AppDeepLinkCommand? {
        val chatUsername = intent.getStringExtra(EXTRA_OPEN_CHAT)
        if (chatUsername.isNullOrBlank()) return null

        intent.removeExtra(EXTRA_OPEN_CHAT)
        return AppDeepLinkCommand(
            route = AppRoute.Chat(
                chatId = chatUsername,
                displayName = chatUsername,
            ),
            notificationClearTarget = NotificationClearTarget.PrivateChat(chatUsername),
        )
    }

    private fun parseGroupChat(intent: Intent): AppDeepLinkCommand? {
        val data = intent.data
        val deepLinkGroupId = data?.lastPathSegment?.toIntOrNull() ?: -1
        val deepLinkGroupName = data?.getQueryParameter(QUERY_GROUP_NAME)
        val groupId = if (deepLinkGroupId > 0) {
            deepLinkGroupId
        } else {
            intent.getIntExtra(EXTRA_OPEN_GROUP_ID, -1)
        }
        if (groupId <= 0) return null

        val groupName = deepLinkGroupName ?: intent.getStringExtra(EXTRA_OPEN_GROUP_NAME)
        intent.removeExtra(EXTRA_OPEN_GROUP_ID)
        intent.removeExtra(EXTRA_OPEN_GROUP_NAME)
        return AppDeepLinkCommand(
            route = AppRoute.GroupChat(
                groupId = groupId,
                groupName = groupName,
            ),
            notificationClearTarget = NotificationClearTarget.GroupChat(groupId),
        )
    }

    private fun parseActivityNotifications(intent: Intent): AppDeepLinkCommand? {
        val openActivity = intent.getBooleanExtra(EXTRA_OPEN_ACTIVITY_NOTIFICATIONS, false)
        if (!openActivity) return null

        intent.removeExtra(EXTRA_OPEN_ACTIVITY_NOTIFICATIONS)
        return AppDeepLinkCommand(route = AppRoute.ActivityNotification)
    }

    companion object {
        const val EXTRA_OPEN_CHAT = "open_chat"
        const val EXTRA_OPEN_GROUP_ID = "open_group_id"
        const val EXTRA_OPEN_GROUP_NAME = "open_group_name"
        const val EXTRA_OPEN_ACTIVITY_NOTIFICATIONS = "open_activity_notifications"
        private const val QUERY_GROUP_NAME = "name"
    }
}
