package com.dino.message.spashscreen.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.dino.message.BuildConfig
import com.dino.message.DinoOrderApplication
import com.dino.message.authfeature.domain.usecase.SyncDeviceIdUseCase
import com.dino.message.corefeature.data.spref.DataStoreUtil
import com.dino.message.corefeature.data.spref.SPrefManager
import com.dino.message.corefeature.domain.model.AppLanguage
import com.dino.message.corefeature.domain.model.StringResourceContent
import com.dino.message.corefeature.presentation.util.PopBackStackLevel
import com.dino.message.corefeature.presentation.util.Resource
import com.dino.message.corefeature.presentation.viewmodel.HaveUIEvent
import com.dino.message.corefeature.presentation.viewmodel.HaveUIEventImpl
import com.dino.message.profilefeature.domain.usecase.GetMyProfileUseCase
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@HiltViewModel
class SpashViewModel @Inject constructor(
    @ApplicationContext app: Context,
    private val dataStoreUtil: DataStoreUtil,
    private val sPrefManager: SPrefManager,
    private val getMyProfileUseCase: GetMyProfileUseCase,
    private val splashApiService: SplashApiService,
    private val syncDeviceIdUseCase: SyncDeviceIdUseCase,
) : AndroidViewModel(app as Application),
    HaveUIEvent by HaveUIEventImpl(app as DinoOrderApplication) {

    private val _uiState = MutableStateFlow(SplashUiState())
    val uiState: StateFlow<SplashUiState> = _uiState.asStateFlow()
    private var hasLanguageSelected = false
    private var hasNavigated = false
    private var pendingRoutingDecision: RoutingDecision? = null

    private companion object {
        const val BADGE_CACHE_TTL_MILLIS = 24L * 60L * 60L * 1000L
    }

    init {
        initializeStartup()
    }

    fun retryVersionCheck() {
        checkVersionAndPrepareRouting()
    }

    fun onLanguageSelected(appLanguage: AppLanguage) {
        if (_uiState.value.isSavingLanguage || hasLanguageSelected) return
        _uiState.value = _uiState.value.copy(
            isSavingLanguage = true,
            errorMessage = null,
        )
        viewModelScope.launch {
            dataStoreUtil.saveLanguage(appLanguage)
            dataStoreUtil.setLanguageOnboardingCompleted(true)
            hasLanguageSelected = true
            _uiState.value = _uiState.value.copy(
                showLanguageSelection = false,
                isSavingLanguage = false,
            )
            attemptNavigateIfReady()
        }
    }

    private fun initializeStartup() {
        viewModelScope.launch {
            hasLanguageSelected = dataStoreUtil.hasCompletedLanguageOnboarding().first()
            _uiState.value = SplashUiState(
                isChecking = true,
                showLanguageSelection = !hasLanguageSelected,
            )
            checkVersionAndPrepareRouting()
        }
    }

    private fun checkVersionAndPrepareRouting() {
        _uiState.value = _uiState.value.copy(
            isChecking = true,
            errorMessage = null,
        )
        viewModelScope.launch {
            val appVersion = normalizeVersionName(BuildConfig.VERSION_NAME)
            when (val response = splashApiService.checkAppVersion(version = appVersion)) {
                is Resource.Success -> {
                    val decision = response.data
                    pendingRoutingDecision = if (decision.action == "force" || decision.action == "optional") {
                        RoutingDecision.AppUpdate(
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
                        )
                    } else {
                        RoutingDecision.NextScreen
                    }
                    _uiState.value = _uiState.value.copy(
                        isChecking = false,
                        errorMessage = null,
                    )
                    attemptNavigateIfReady()
                }

                is Resource.Error -> {
                    pendingRoutingDecision = null
                    _uiState.value = _uiState.value.copy(
                        isChecking = false,
                        errorMessage = response.message,
                    )
                }

                is Resource.Loading -> {
                    _uiState.value = _uiState.value.copy(
                        isChecking = true,
                        errorMessage = null,
                    )
                }
            }
        }
    }

    private fun attemptNavigateIfReady() {
        if (hasNavigated || !hasLanguageSelected) return
        val decision = pendingRoutingDecision ?: return
        hasNavigated = true
        when (decision) {
            is RoutingDecision.AppUpdate -> {
                navigateWithPopBackStackToDestination(
                    direction = AppUpdateScreenDestination(
                        isForceUpdate = decision.isForceUpdate,
                        title = decision.title,
                        message = decision.message,
                        downloadUrl = decision.downloadUrl,
                    ),
                    scope = viewModelScope,
                    level = PopBackStackLevel.ALL,
                )
            }

            RoutingDecision.NextScreen -> {
                prepareSessionAndNavigate()
            }
        }
    }

    private fun prepareSessionAndNavigate() = viewModelScope.launch {
        if (sPrefManager.getUserLogIn()) {
            _uiState.value = _uiState.value.copy(isChecking = true)
            refreshBadgeCacheIfNeeded()
            syncDeviceIdBestEffort()
            _uiState.value = _uiState.value.copy(isChecking = false)
        } else {
            dataStoreUtil.clearBadgeAccessCache()
        }
        navigateToNextScreen()
    }

    private suspend fun refreshBadgeCacheIfNeeded() {
        val lastSyncEpochMillis = dataStoreUtil.getBadgeAccessLastSyncEpochMillis().first()
        val now = System.currentTimeMillis()
        val isCacheFresh = (now - lastSyncEpochMillis) in 0 until BADGE_CACHE_TTL_MILLIS
        if (isCacheFresh) return

        when (val res = getMyProfileUseCase.execute().first { it !is Resource.Loading }) {
            is Resource.Success -> {
                dataStoreUtil.saveBadgeAccessCache(
                    hasBadge = !res.data.badge.isNullOrBlank(),
                    syncedAtEpochMillis = now
                )
            }

            is Resource.Error -> {
                // Keep previous cached value if refresh fails while offline or server is unavailable.
            }

            is Resource.Loading -> Unit
        }
    }

    private sealed interface RoutingDecision {
        data class AppUpdate(
            val isForceUpdate: Boolean,
            val title: String,
            val message: String,
            val downloadUrl: String,
        ) : RoutingDecision

        data object NextScreen : RoutingDecision
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
                direction = ThreadScreenDestination(),
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

    private suspend fun syncDeviceIdBestEffort() {
        syncDeviceIdUseCase.execute().collect { _ -> }
    }
}

data class SplashUiState(
    val isChecking: Boolean = true,
    val errorMessage: StringResourceContent? = null,
    val showLanguageSelection: Boolean = false,
    val isSavingLanguage: Boolean = false,
)
