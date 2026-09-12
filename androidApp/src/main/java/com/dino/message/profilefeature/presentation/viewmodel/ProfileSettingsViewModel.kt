package com.dino.message.profilefeature.presentation.viewmodel

import com.dino.message.corefeature.presentation.util.asString
import com.dino.message.corefeature.domain.settings.AppSettingsStore
import com.dino.message.corefeature.domain.model.AppLanguage
import com.dino.message.corefeature.domain.usecase.ClearLocalSessionUseCase
import com.dino.message.corefeature.presentation.util.PopBackStackLevel
import com.dino.message.corefeature.domain.result.Resource
import com.dino.message.corefeature.domain.resource.StringResolver
import com.dino.message.corefeature.presentation.viewmodel.HaveUIEvent
import com.dino.message.corefeature.presentation.viewmodel.HaveUIEventImpl
import com.dino.message.corefeature.presentation.util.UiEventBus
import com.dino.message.corefeature.presentation.viewmodel.ScopedStateHolder
import com.dino.message.profilefeature.domain.usecase.DeleteMyAccountUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.dino.message.corefeature.presentation.navigation.AppRoute

class ProfileSettingsViewModel constructor(
    private val stringResolver: StringResolver,
    uiEventBus: UiEventBus,
    private val deleteMyAccountUseCase: DeleteMyAccountUseCase,
    private val clearLocalSessionUseCase: ClearLocalSessionUseCase,
    private val dataStoreUtil: AppSettingsStore
) : ScopedStateHolder(),
    HaveUIEvent by HaveUIEventImpl(uiEventBus) {

    private val _showDeleteAccountDialog = MutableStateFlow(false)
    val showDeleteAccountDialog = _showDeleteAccountDialog.asStateFlow()

    private val _deleteAccountLoading = MutableStateFlow(false)
    val deleteAccountLoading = _deleteAccountLoading.asStateFlow()

    private val _deleteAccountError = MutableStateFlow<String?>(null)
    val deleteAccountError = _deleteAccountError.asStateFlow()

    private val _logoutLoading = MutableStateFlow(false)
    val logoutLoading = _logoutLoading.asStateFlow()
    val currentLanguage: Flow<AppLanguage> = dataStoreUtil.getLanguage()

    fun openBlockList() {
        navigateToDestination(AppRoute.BlockList, viewModelScope)
    }

    fun openPrivateKeyResetDialog() {
        navigateToDestination(AppRoute.PrivateKeyResetDialog, viewModelScope)
    }

    fun openAboutUs() {
        navigateToDestination(AppRoute.AboutUs, viewModelScope)
    }

    fun openDeleteAccountDialog() {
        if (_deleteAccountLoading.value || _logoutLoading.value) return
        _deleteAccountError.value = null
        _showDeleteAccountDialog.value = true
    }

    fun dismissDeleteAccountDialog() {
        if (_deleteAccountLoading.value || _logoutLoading.value) return
        _showDeleteAccountDialog.value = false
        _deleteAccountError.value = null
    }

    fun confirmDeleteAccount() {
        if (_deleteAccountLoading.value || _logoutLoading.value) return
        viewModelScope.launch {
            deleteMyAccountUseCase.execute().collect { res ->
                when (res) {
                    is Resource.Loading -> {
                        _deleteAccountLoading.value = true
                        _deleteAccountError.value = null
                    }

                    is Resource.Success -> {
                        _deleteAccountLoading.value = false
                        _showDeleteAccountDialog.value = false
                        logoutAndClearSession()
                    }

                    is Resource.Error -> {
                        _deleteAccountLoading.value = false
                        _deleteAccountError.value = res.message.resolveWith(stringResolver)
                    }
                }
            }
        }
    }

    fun logout() {
        if (_logoutLoading.value || _deleteAccountLoading.value) return
        viewModelScope.launch {
            _logoutLoading.value = true
            _showDeleteAccountDialog.value = false
            logoutAndClearSession()
            _logoutLoading.value = false
        }
    }

    fun setLanguage(appLanguage: AppLanguage) {
        viewModelScope.launch {
            dataStoreUtil.saveLanguage(appLanguage)
        }
    }

    private suspend fun logoutAndClearSession() {
        clearLocalSessionUseCase.execute()
        navigateWithPopBackStackToDestination(
            AppRoute.Login,
            viewModelScope,
            PopBackStackLevel.ALL
        )
    }

    fun navigateUp() {
        navigateUp(viewModelScope)
    }
}
