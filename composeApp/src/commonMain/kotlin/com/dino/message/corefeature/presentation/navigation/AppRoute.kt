package com.dino.message.corefeature.presentation.navigation

import kotlinx.serialization.Serializable

@Serializable
sealed interface AppRoute {
    @Serializable
    sealed interface Screen : AppRoute

    @Serializable
    sealed interface Modal : AppRoute

    @Serializable
    data object Splash : Screen

    @Serializable
    data object Main : Screen

    @Serializable
    data object Login : Screen

    @Serializable
    data object Register : Screen

    @Serializable
    data object PrivateKeyAlert : Screen

    @Serializable
    data object Thread : Screen

    @Serializable
    data class ThreadWithStory(val initialStoryId: Long? = null) : Screen

    @Serializable
    data object Search : Screen

    @Serializable
    data object ActivityNotification : Screen

    @Serializable
    data class Chat(
        val chatId: String,
        val displayName: String? = null,
        val badge: String? = null,
        val profileImageUrl: String? = null,
        val profileImageShape: String? = null,
        val profileVideoUrl: String? = null,
    ) : Screen

    @Serializable
    data object CreateGroup : Screen

    @Serializable
    data class GroupChat(
        val groupId: Int,
        val groupName: String? = null,
        val isPublicRoom: Boolean = false,
    ) : Screen

    @Serializable
    data class GroupMembers(
        val groupId: Int,
        val groupName: String? = null,
    ) : Screen

    @Serializable
    data class AddGroupMembers(
        val groupId: Int,
        val groupName: String? = null,
    ) : Screen

    @Serializable
    data class Profile(val username: String? = null) : Screen

    @Serializable
    data object EditProfile : Screen

    @Serializable
    data object ProfileSettings : Screen

    @Serializable
    data object BlockList : Screen

    @Serializable
    data object AboutUs : Screen

    @Serializable
    data object Referral : Screen

    @Serializable
    data class ProfileFollowList(
        val username: String,
        val showFollowers: Boolean,
    ) : Screen

    @Serializable
    data class SharedPostDetails(val postId: Long) : Screen

    @Serializable
    data class CreatePost(
        val quotedPostId: Long = 0L,
        val editPostId: Long = 0L,
    ) : Screen

    @Serializable
    data class PostMusic(
        val quotedPostId: Long = 0L,
        val editPostId: Long = 0L,
    ) : Screen

    @Serializable
    data object PlaylistSheet : Modal

    @Serializable
    data class CreatePostOptionsSheet(val quotedPostId: Long = 0L) : Modal

    @Serializable
    data class ThreadCommentsSheet(val postId: Long) : Modal

    @Serializable
    data class SharePostSheet(
        val postId: Long,
        val authorUsername: String,
        val postText: String,
    ) : Modal

    @Serializable
    data class ReportPostSheet(val postId: Long) : Modal

    @Serializable
    data class DeletePostSheet(val postId: Long) : Modal

    @Serializable
    data object PrivateKeyResetDialog : Modal

    @Serializable
    data class TrackPlaylistAddersDialog(
        val trackTitle: String,
        val playlistAddersJson: String,
    ) : Modal

    @Serializable
    data class AppUpdate(
        val isForceUpdate: Boolean,
        val title: String,
        val message: String,
        val downloadUrl: String,
    ) : Screen
}
