package com.dino.message.corefeature.presentation.navigation

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavGraph(
    navController: NavHostController,
    activeModalRoute: MutableState<AppRoute?>,
    render: @Composable (AppRoute) -> Unit,
) {
    NavHost(navController = navController, startDestination = AppRoute.Splash) {
        composable<AppRoute.Splash> { render(AppRoute.Splash) }
        composable<AppRoute.Main> { render(AppRoute.Main) }
        composable<AppRoute.Login> { render(AppRoute.Login) }
        composable<AppRoute.Register> { render(AppRoute.Register) }
        composable<AppRoute.PrivateKeyAlert> { render(AppRoute.PrivateKeyAlert) }
        composable<AppRoute.Thread> { render(AppRoute.Thread) }
        composable<AppRoute.ThreadWithStory> { render(it.toRoute<AppRoute.ThreadWithStory>()) }
        composable<AppRoute.Search> { render(AppRoute.Search) }
        composable<AppRoute.ActivityNotification> { render(AppRoute.ActivityNotification) }
        composable<AppRoute.Chat> { render(it.toRoute<AppRoute.Chat>()) }
        composable<AppRoute.CreateGroup> { render(AppRoute.CreateGroup) }
        composable<AppRoute.GroupChat> { render(it.toRoute<AppRoute.GroupChat>()) }
        composable<AppRoute.GroupMembers> { render(it.toRoute<AppRoute.GroupMembers>()) }
        composable<AppRoute.AddGroupMembers> { render(it.toRoute<AppRoute.AddGroupMembers>()) }
        composable<AppRoute.Profile> { render(it.toRoute<AppRoute.Profile>()) }
        composable<AppRoute.EditProfile> { render(AppRoute.EditProfile) }
        composable<AppRoute.ProfileSettings> { render(AppRoute.ProfileSettings) }
        composable<AppRoute.BlockList> { render(AppRoute.BlockList) }
        composable<AppRoute.AboutUs> { render(AppRoute.AboutUs) }
        composable<AppRoute.Referral> { render(AppRoute.Referral) }
        composable<AppRoute.ProfileFollowList> { render(it.toRoute<AppRoute.ProfileFollowList>()) }
        composable<AppRoute.SharedPostDetails> { render(it.toRoute<AppRoute.SharedPostDetails>()) }
        composable<AppRoute.CreatePost> { render(it.toRoute<AppRoute.CreatePost>()) }
        composable<AppRoute.PostMusic> { render(it.toRoute<AppRoute.PostMusic>()) }
        composable<AppRoute.AppUpdate> { render(it.toRoute<AppRoute.AppUpdate>()) }
    }

    val modal = activeModalRoute.value as? AppRoute.Modal ?: return
    val dismiss = { activeModalRoute.value = null }
    when (modal) {
        AppRoute.PrivateKeyResetDialog, is AppRoute.TrackPlaylistAddersDialog -> {
            Dialog(onDismissRequest = dismiss) { render(modal) }
        }
        else -> {
            ModalBottomSheet(onDismissRequest = dismiss, modifier = Modifier.fillMaxWidth()) {
                render(modal)
            }
        }
    }
}
