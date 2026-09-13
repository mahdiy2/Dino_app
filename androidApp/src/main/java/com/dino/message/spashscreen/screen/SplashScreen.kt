package com.dino.message.spashscreen.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.NavHostController
import com.dino.message.corefeature.presentation.viewmodel.rememberStateHolder
import com.dino.message.spashscreen.viewmodel.SpashViewModel

@Composable
fun SplashScreen(
    @Suppress("UNUSED_PARAMETER")
    navController: NavHostController,
) {
    val viewModel: SpashViewModel = rememberStateHolder()
    val uiState by viewModel.uiState.collectAsState()

    SplashContent(
        uiState = uiState,
        onLanguageSelected = viewModel::onLanguageSelected,
        onRetryVersionCheck = viewModel::retryVersionCheck,
    )
}
