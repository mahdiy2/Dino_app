package com.dino.message.corefeature.presentation.navigation

import kotlinx.serialization.Serializable

@Serializable
sealed interface AppRoute {
    @Serializable
    data object Splash : AppRoute

    @Serializable
    data object Main : AppRoute

    @Serializable
    data object Login : AppRoute

    @Serializable
    data object Register : AppRoute

    @Serializable
    data object PrivateKeyAlert : AppRoute

    @Serializable
    data object Thread : AppRoute

    @Serializable
    data class ThreadWithStory(val initialStoryId: Long? = null) : AppRoute

    @Serializable
    data object Search : AppRoute

    @Serializable
    data object ActivityNotification : AppRoute

    @Serializable
    data class Chat(
        val chatId: String,
        val displayName: String? = null,
        val badge: String? = null,
        val profileImageUrl: String? = null,
        val profileImageShape: String? = null,
        val profileVideoUrl: String? = null,
    ) : AppRoute

    @Serializable
    data object CreateGroup : AppRoute

    @Serializable
    data class GroupChat(
        val groupId: Int,
        val groupName: String? = null,
        val isPublicRoom: Boolean = false,
    ) : AppRoute

    @Serializable
    data class GroupMembers(
        val groupId: Int,
        val groupName: String? = null,
    ) : AppRoute

    @Serializable
    data class AddGroupMembers(
        val groupId: Int,
        val groupName: String? = null,
    ) : AppRoute

    @Serializable
    data class Profile(val username: String? = null) : AppRoute

    @Serializable
    data object EditProfile : AppRoute

    @Serializable
    data object ProfileSettings : AppRoute

    @Serializable
    data object BlockList : AppRoute

    @Serializable
    data object AboutUs : AppRoute

    @Serializable
    data object Referral : AppRoute

    @Serializable
    data class ProfileFollowList(
        val username: String,
        val showFollowers: Boolean,
    ) : AppRoute

    @Serializable
    data class SharedPostDetails(val postId: Long) : AppRoute

    @Serializable
    data class CreatePost(
        val quotedPostId: Long = 0L,
        val editPostId: Long = 0L,
    ) : AppRoute

    @Serializable
    data class PostMusic(
        val quotedPostId: Long = 0L,
        val editPostId: Long = 0L,
    ) : AppRoute

    @Serializable
    data object PlaylistSheet : AppRoute

    @Serializable
    data class CreatePostOptionsSheet(val quotedPostId: Long = 0L) : AppRoute

    @Serializable
    data class ThreadCommentsSheet(val postId: Long) : AppRoute

    @Serializable
    data class SharePostSheet(
        val postId: Long,
        val authorUsername: String,
        val postText: String,
    ) : AppRoute

    @Serializable
    data class ReportPostSheet(val postId: Long) : AppRoute

    @Serializable
    data class DeletePostSheet(val postId: Long) : AppRoute

    @Serializable
    data object PrivateKeyResetDialog : AppRoute

    @Serializable
    data class TrackPlaylistAddersDialog(
        val trackTitle: String,
        val playlistAddersJson: String,
    ) : AppRoute

    @Serializable
    data class AppUpdate(
        val isForceUpdate: Boolean,
        val title: String,
        val message: String,
        val downloadUrl: String,
    ) : AppRoute
}
