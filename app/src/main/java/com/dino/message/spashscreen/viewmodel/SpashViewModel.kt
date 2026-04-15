package com.dino.message.spashscreen.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.dino.message.BuildConfig
import com.dino.message.DinoOrderApplication
import com.dino.message.corefeature.data.spref.SPrefManager
import com.dino.message.corefeature.domain.model.StringResourceContent
import com.dino.message.corefeature.presentation.util.PopBackStackLevel
import com.dino.message.corefeature.presentation.util.Resource
import com.dino.message.corefeature.presentation.viewmodel.HaveUIEvent
import com.dino.message.corefeature.presentation.viewmodel.HaveUIEventImpl
import com.dino.message.spashscreen.data.remote.SplashApiService
import com.ramcosta.composedestinations.generated.destinations.AppUpdateScreenDestination
import com.ramcosta.composedestinations.generated.destinations.PrivateKeyAlertScreenDestination
import com.ramcosta.composedestinations.generated.destinations.RegisterScreenDestination
import com.ramcosta.composedestinations.generated.destinations.ThreadScreenDestination
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class SpashViewModel @Inject constructor(
    @ApplicationContext app: Context,
    private val sPrefManager: SPrefManager,
    private val splashApiService: SplashApiService,
) : AndroidViewModel(app as Application),
    HaveUIEvent by HaveUIEventImpl(app as DinoOrderApplication) {

    private val _uiState = MutableStateFlow(SplashUiState())
    val uiState: StateFlow<SplashUiState> = _uiState.asStateFlow()

    init {
        checkVersionAndRoute()
    }

    fun retryVersionCheck() {
        checkVersionAndRoute()
    }

    private fun checkVersionAndRoute() {
        _uiState.value = SplashUiState(isChecking = true, errorMessage = null)
        viewModelScope.launch {
            val appVersion = normalizeVersionName(BuildConfig.VERSION_NAME)
            when (val response = splashApiService.checkAppVersion(version = appVersion)) {
                is Resource.Success -> {
                    val decision = response.data
                    if (decision.action == "force" || decision.action == "optional") {
                        navigateWithPopBackStackToDestination(
                            direction = AppUpdateScreenDestination(
                                isForceUpdate = decision.action == "force",
                                title = if (decision.title.isBlank()) {
                                    if (decision.action == "force") "Update required" else "Update available"
                                } else {
                                    decision.title
                                },
                                message = if (decision.message.isBlank()) {
                                    if (decision.action == "force") {
                                        "Please update to continue using the app."
                                    } else {
                                        "A newer version is available. You can update now or later."
                                    }
                                } else {
                                    decision.message
                                },
                                downloadUrl = resolveDownloadUrl(decision.downloadUrl),
                            ),
                            scope = viewModelScope,
                            level = PopBackStackLevel.ALL,
                        )
                    } else {
                        navigateToNextScreen()
                    }
                }

                is Resource.Error -> {
                    _uiState.value = SplashUiState(
                        isChecking = false,
                        errorMessage = response.message,
                    )
                }

                is Resource.Loading -> {
                    _uiState.value = SplashUiState(isChecking = true, errorMessage = null)
                }
            }
        }
    }

    private fun resolveDownloadUrl(rawUrl: String): String {
        val trimmed = rawUrl.trim()
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            return trimmed
        }

        val path = if (trimmed.isBlank()) "download/app" else trimmed.trimStart('/')
        return BuildConfig.BASE_URL.trimEnd('/') + "/" + path
    }

    private fun normalizeVersionName(rawVersion: String): String {
        val extracted = Regex("""\d+(?:\.\d+){0,2}""")
            .find(rawVersion)
            ?.value
            .orEmpty()

        if (extracted.isBlank()) return "0.0.0"

        val parts = extracted
            .split('.')
            .mapNotNull { it.toIntOrNull() }
            .toMutableList()

        while (parts.size < 3) {
            parts.add(0)
        }

        return parts.take(3).joinToString(".")
    }

    fun navigateToNextScreen() {
        val userLogIn = sPrefManager.getUserLogIn()

        if (userLogIn) {
            navigateWithPopBackStackToDestination(
                direction = ThreadScreenDestination,
                scope = viewModelScope,
                level = PopBackStackLevel.ALL,
            )
        } else if (sPrefManager.hasPendingRegistration()) {
            navigateWithPopBackStackToDestination(
                direction = PrivateKeyAlertScreenDestination,
                scope = viewModelScope,
                level = PopBackStackLevel.ALL,
            )
        } else {
            navigateWithPopBackStackToDestination(
                direction = RegisterScreenDestination,
                scope = viewModelScope,
                level = PopBackStackLevel.ALL,
            )
        }
    }
}

data class SplashUiState(
    val isChecking: Boolean = true,
    val errorMessage: StringResourceContent? = null,
)
