package com.dino.message.corefeature.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.navigation.NavHostController
import com.dino.message.corefeature.presentation.util.HandleUIEvent
import com.dino.message.corefeature.presentation.util.UiEventBus
import com.dino.message.corefeature.presentation.viewmodel.LocalStateHolderFactory
import com.dino.message.corefeature.presentation.viewmodel.StateHolderFactory

@Composable
fun AppShell(
    navController: NavHostController,
    uiEventBus: UiEventBus,
    stateHolderFactory: StateHolderFactory,
    render: @Composable (AppRoute) -> Unit,
) {
    val modal = remember { mutableStateOf<AppRoute?>(null) }
    CompositionLocalProvider(LocalStateHolderFactory provides stateHolderFactory) {
        HandleUIEvent(uiEventBus, navController, modal)
        AppNavGraph(navController, modal, render)
    }
}
