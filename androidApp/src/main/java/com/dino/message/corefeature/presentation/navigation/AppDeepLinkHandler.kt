package com.dino.message.corefeature.presentation.navigation

data class AppDeepLinkCommand(
    val route: AppRoute,
    val notificationClearTarget: NotificationClearTarget? = null,
)

sealed class NotificationClearTarget {
    data class PrivateChat(val username: String) : NotificationClearTarget()
    data class GroupChat(val groupId: Int) : NotificationClearTarget()
}

interface AppDeepLinkHandler<PlatformInput> {
    fun consume(input: PlatformInput?): AppDeepLinkCommand?
}
