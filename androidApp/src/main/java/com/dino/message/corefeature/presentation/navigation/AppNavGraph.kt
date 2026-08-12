package com.dino.message.corefeature.presentation.navigation

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Modifier
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.runtime.Composable
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
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
fun AppNavGraph(
    navController: NavHostController,
    activeModalRoute: MutableState<AppRoute?>,
) {
    NavHost(
        navController = navController,
        startDestination = AppRoute.Splash,
    ) {
        composable<AppRoute.Splash> { SplashScreen(navController) }
        composable<AppRoute.Main> { MainScreen() }
        composable<AppRoute.Login> { LoginScreen() }
        composable<AppRoute.Register> { RegisterScreen() }
        composable<AppRoute.PrivateKeyAlert> { PrivateKeyAlertScreen() }
        composable<AppRoute.Thread> { ThreadScreen() }
        composable<AppRoute.ThreadWithStory> { entry ->
            val route = entry.toRoute<AppRoute.ThreadWithStory>()
            ThreadScreen(initialStoryId = route.initialStoryId)
        }
        composable<AppRoute.Search> { SearchScreen() }
        composable<AppRoute.ActivityNotification> { ActivityNotificationScreen() }
        composable<AppRoute.Chat> { entry ->
            val route = entry.toRoute<AppRoute.Chat>()
            ChatScreen(
                chatId = route.chatId,
                displayName = route.displayName,
                badge = route.badge,
                profileImageUrl = route.profileImageUrl,
                profileImageShape = route.profileImageShape,
                profileVideoUrl = route.profileVideoUrl,
            )
        }
        composable<AppRoute.CreateGroup> { CreateGroupScreen() }
        composable<AppRoute.GroupChat> { entry ->
            val route = entry.toRoute<AppRoute.GroupChat>()
            GroupChatScreen(route.groupId, route.groupName, route.isPublicRoom)
        }
        composable<AppRoute.GroupMembers> { entry ->
            val route = entry.toRoute<AppRoute.GroupMembers>()
            GroupMembersScreen(route.groupId, route.groupName)
        }
        composable<AppRoute.AddGroupMembers> { entry ->
            val route = entry.toRoute<AppRoute.AddGroupMembers>()
            AddGroupMembersScreen(route.groupId, route.groupName)
        }
        composable<AppRoute.Profile> { entry ->
            ProfileScreen(entry.toRoute<AppRoute.Profile>().username)
        }
        composable<AppRoute.EditProfile> { EditProfileScreen() }
        composable<AppRoute.ProfileSettings> { ProfileSettingsScreen() }
        composable<AppRoute.BlockList> { BlockListScreen() }
        composable<AppRoute.AboutUs> { AboutUsScreen() }
        composable<AppRoute.Referral> { ReferralScreen() }
        composable<AppRoute.ProfileFollowList> { entry ->
            val route = entry.toRoute<AppRoute.ProfileFollowList>()
            ProfileFollowListScreen(route.username, route.showFollowers)
        }
        composable<AppRoute.SharedPostDetails> { entry ->
            SharedPostDetailsScreen(entry.toRoute<AppRoute.SharedPostDetails>().postId)
        }
        composable<AppRoute.CreatePost> { entry ->
            val route = entry.toRoute<AppRoute.CreatePost>()
            CreatePostScreen(route.quotedPostId, route.editPostId)
        }
        composable<AppRoute.PostMusic> { entry ->
            val route = entry.toRoute<AppRoute.PostMusic>()
            PostMusicScreen(route.quotedPostId, route.editPostId)
        }
        composable<AppRoute.AppUpdate> { entry ->
            val route = entry.toRoute<AppRoute.AppUpdate>()
            AppUpdateScreen(route.isForceUpdate, route.title, route.message, route.downloadUrl)
        }
    }


    AppModalHost(activeModalRoute = activeModalRoute)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppModalHost(activeModalRoute: MutableState<AppRoute?>) {
    when (val route = activeModalRoute.value) {
        AppRoute.PlaylistSheet -> {
            ModalBottomSheet(
                onDismissRequest = { activeModalRoute.value = null },
                modifier = Modifier.fillMaxWidth(),
            ) {
                PlaylistBottomSheetScreen()
            }
        }
        is AppRoute.CreatePostOptionsSheet -> {
            ModalBottomSheet(
                onDismissRequest = { activeModalRoute.value = null },
                modifier = Modifier.fillMaxWidth(),
            ) {
                CreatePostOptionsBottomSheetScreen(route.quotedPostId)
            }
        }
        is AppRoute.ThreadCommentsSheet -> {
            ModalBottomSheet(
                onDismissRequest = { activeModalRoute.value = null },
                modifier = Modifier.fillMaxWidth(),
            ) {
                ThreadCommentsBottomSheetScreen(route.postId)
            }
        }
        is AppRoute.SharePostSheet -> {
            ModalBottomSheet(
                onDismissRequest = { activeModalRoute.value = null },
                modifier = Modifier.fillMaxWidth(),
            ) {
                SharePostBottomSheetScreen(route.postId, route.authorUsername, route.postText)
            }
        }
        is AppRoute.ReportPostSheet -> {
            ModalBottomSheet(
                onDismissRequest = { activeModalRoute.value = null },
                modifier = Modifier.fillMaxWidth(),
            ) {
                ReportPostBottomSheetScreen(route.postId)
            }
        }
        is AppRoute.DeletePostSheet -> {
            ModalBottomSheet(
                onDismissRequest = { activeModalRoute.value = null },
                modifier = Modifier.fillMaxWidth(),
            ) {
                DeletePostBottomSheetScreen(route.postId)
            }
        }
        AppRoute.PrivateKeyResetDialog -> {
            Dialog(onDismissRequest = { activeModalRoute.value = null }) {
                PrivateKeyResetDialogScreen()
            }
        }
        is AppRoute.TrackPlaylistAddersDialog -> {
            Dialog(onDismissRequest = { activeModalRoute.value = null }) {
                TrackPlaylistAddersDialogScreen(route.trackTitle, route.playlistAddersJson)
            }
        }
        null -> Unit
        else -> Unit
    }
}
