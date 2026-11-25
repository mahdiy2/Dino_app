package com.dino.message.corefeature.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import com.dino.message.activitynotificationfeature.presentation.screen.ActivityNotificationScreen
import com.dino.message.authfeature.presentation.screen.LoginScreen
import com.dino.message.authfeature.presentation.screen.PrivateKeyAlertScreen
import com.dino.message.authfeature.presentation.screen.RegisterScreen
import com.dino.message.chatfeature.presentation.screen.ChatScreen
import com.dino.message.corefeature.presentation.screen.PrivateKeyResetDialogScreen
import com.dino.message.groupfeature.presentation.screen.AddGroupMembersScreen
import com.dino.message.groupfeature.presentation.screen.CreateGroupScreen
import com.dino.message.groupfeature.presentation.screen.GroupChatScreen
import com.dino.message.groupfeature.presentation.screen.GroupMembersScreen
import com.dino.message.mainpage.presentation.screen.MainScreen
import com.dino.message.profilefeature.presentation.screen.AboutUsScreen
import com.dino.message.profilefeature.presentation.screen.BlockListScreen
import com.dino.message.profilefeature.presentation.screen.DeletePostBottomSheetScreen
import com.dino.message.profilefeature.presentation.screen.EditProfileScreen
import com.dino.message.profilefeature.presentation.screen.ProfileFollowListScreen
import com.dino.message.profilefeature.presentation.screen.ProfileScreen
import com.dino.message.profilefeature.presentation.screen.ProfileSettingsScreen
import com.dino.message.profilefeature.presentation.screen.ReferralScreen
import com.dino.message.reportfeature.presentation.screen.ReportPostBottomSheetScreen
import com.dino.message.searchfeature.presentation.screen.SearchScreen
import com.dino.message.spashscreen.screen.SplashScreen
import com.dino.message.threadfeature.presentation.screen.CreatePostOptionsBottomSheetScreen
import com.dino.message.threadfeature.presentation.screen.CreatePostScreen
import com.dino.message.threadfeature.presentation.screen.PlaylistBottomSheetScreen
import com.dino.message.threadfeature.presentation.screen.PostMusicScreen
import com.dino.message.threadfeature.presentation.screen.SharePostBottomSheetScreen
import com.dino.message.threadfeature.presentation.screen.SharedPostDetailsScreen
import com.dino.message.threadfeature.presentation.screen.ThreadCommentsBottomSheetScreen
import com.dino.message.threadfeature.presentation.screen.ThreadScreen
import com.dino.message.threadfeature.presentation.screen.TrackPlaylistAddersDialogScreen
import com.dino.message.updatefeature.presentation.screen.AppUpdateScreen

@Composable
fun AndroidRouteContent(navController: NavHostController, route: AppRoute) {
    when (route) {
        AppRoute.Splash -> SplashScreen(navController)
        AppRoute.Main -> MainScreen()
        AppRoute.Login -> LoginScreen()
        AppRoute.Register -> RegisterScreen()
        AppRoute.PrivateKeyAlert -> PrivateKeyAlertScreen()
        AppRoute.Thread -> ThreadScreen()
        is AppRoute.ThreadWithStory -> ThreadScreen(initialStoryId = route.initialStoryId)
        AppRoute.Search -> SearchScreen()
        AppRoute.ActivityNotification -> ActivityNotificationScreen()
        is AppRoute.Chat -> ChatScreen(
            chatId = route.chatId,
            displayName = route.displayName,
            badge = route.badge,
            profileImageUrl = route.profileImageUrl,
            profileImageShape = route.profileImageShape,
            profileVideoUrl = route.profileVideoUrl,
        )
        AppRoute.CreateGroup -> CreateGroupScreen()
        is AppRoute.GroupChat -> GroupChatScreen(route.groupId, route.groupName, route.isPublicRoom)
        is AppRoute.GroupMembers -> GroupMembersScreen(route.groupId, route.groupName)
        is AppRoute.AddGroupMembers -> AddGroupMembersScreen(route.groupId, route.groupName)
        is AppRoute.Profile -> ProfileScreen(route.username)
        AppRoute.EditProfile -> EditProfileScreen()
        AppRoute.ProfileSettings -> ProfileSettingsScreen()
        AppRoute.BlockList -> BlockListScreen()
        AppRoute.AboutUs -> AboutUsScreen()
        AppRoute.Referral -> ReferralScreen()
        is AppRoute.ProfileFollowList -> ProfileFollowListScreen(route.username, route.showFollowers)
        is AppRoute.SharedPostDetails -> SharedPostDetailsScreen(route.postId)
        is AppRoute.CreatePost -> CreatePostScreen(route.quotedPostId, route.editPostId)
        is AppRoute.PostMusic -> PostMusicScreen(route.quotedPostId, route.editPostId)
        is AppRoute.AppUpdate -> AppUpdateScreen(route.isForceUpdate, route.title, route.message, route.downloadUrl)
        AppRoute.PlaylistSheet -> PlaylistBottomSheetScreen()
        is AppRoute.CreatePostOptionsSheet -> CreatePostOptionsBottomSheetScreen(route.quotedPostId)
        is AppRoute.ThreadCommentsSheet -> ThreadCommentsBottomSheetScreen(route.postId)
        is AppRoute.SharePostSheet -> SharePostBottomSheetScreen(route.postId, route.authorUsername, route.postText)
        is AppRoute.ReportPostSheet -> ReportPostBottomSheetScreen(route.postId)
        is AppRoute.DeletePostSheet -> DeletePostBottomSheetScreen(route.postId)
        AppRoute.PrivateKeyResetDialog -> PrivateKeyResetDialogScreen()
        is AppRoute.TrackPlaylistAddersDialog -> TrackPlaylistAddersDialogScreen(route.trackTitle, route.playlistAddersJson)
    }
}
