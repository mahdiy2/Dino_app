package com.dino.message.corefeature.presentation.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import com.dino.message.corefeature.presentation.navigation.AppRoute
import com.dino.message.corefeature.presentation.navigation.isModalRoute
import com.dino.message.DinoOrderApplication
import kotlinx.coroutines.flow.collectLatest

@Composable
fun HandleUIEvent(
    app: DinoOrderApplication,
    navController: NavHostController,
    activeModalRoute: MutableState<AppRoute?>,
) {
    LaunchedEffect(key1 = true) {
        app.uIEventFlow.collectLatest { event ->
            when (event) {
                is UIEvent.NavigateWithPopBackStack -> {
                    when (event.level) {
                        is PopBackStackLevel.ALL -> {
                            activeModalRoute.value = null
                            navController.navigate(event.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    inclusive = true
                                }
                                launchSingleTop = true
                            }
                        }
                        is PopBackStackLevel.Number -> {
                            for (index in 0 until event.level.number) {
                                if (!navController.popBackStack()) {
                                    break
                                }
                            }
                            if (event.route.isModalRoute()) {
                                activeModalRoute.value = event.route
                            } else {
                                activeModalRoute.value = null
                                navController.navigate(event.route) {
                                    launchSingleTop = true
                                }
                            }
                        }
                    }
                }
                is UIEvent.Navigate -> {
                    if (event.route.isModalRoute()) {
                        activeModalRoute.value = event.route
                    } else {
                        activeModalRoute.value = null
                        navController.navigate(event.route) {
                            launchSingleTop = true
                        }
                    }
                }
                is UIEvent.NavigateUp -> {
                    if (activeModalRoute.value != null) {
                        activeModalRoute.value = null
                    } else {
                        navController.navigateUp()
                    }
                }
                is UIEvent.ShowCustomMessageDialog -> Unit
                is UIEvent.ShowImageDialog -> Unit
            }
        }
    }
}
