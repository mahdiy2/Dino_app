package com.dino.message.profilefeature.presentation.viewmodel

import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.dino.message.DinoOrderApplication
import com.dino.message.corefeature.domain.usecase.ClearLocalSessionUseCase
import com.dino.message.corefeature.presentation.util.PopBackStackLevel
import com.dino.message.corefeature.presentation.util.Resource
import com.dino.message.corefeature.presentation.viewmodel.HaveUIEvent
import com.dino.message.corefeature.presentation.viewmodel.HaveUIEventImpl
import com.dino.message.profilefeature.domain.usecase.DeleteMyAccountUseCase
import com.ramcosta.composedestinations.generated.destinations.BlockListScreenDestination
import com.ramcosta.composedestinations.generated.destinations.LoginScreenDestination
import com.ramcosta.composedestinations.generated.destinations.PrivateKeyResetDialogScreenDestination
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class ProfileSettingsViewModel @Inject constructor(
    @ApplicationContext app: Context,
    private val deleteMyAccountUseCase: DeleteMyAccountUseCase,
    private val clearLocalSessionUseCase: ClearLocalSessionUseCase
) : AndroidViewModel(app as DinoOrderApplication),
    HaveUIEvent by HaveUIEventImpl(app as DinoOrderApplication) {

    private val _showDeleteAccountDialog = MutableStateFlow(false)
    val showDeleteAccountDialog = _showDeleteAccountDialog.asStateFlow()

    private val _deleteAccountLoading = MutableStateFlow(false)
    val deleteAccountLoading = _deleteAccountLoading.asStateFlow()

    private val _deleteAccountError = MutableStateFlow<String?>(null)
    val deleteAccountError = _deleteAccountError.asStateFlow()

    private val _logoutLoading = MutableStateFlow(false)
    val logoutLoading = _logoutLoading.asStateFlow()

    fun openBlockList() {
        navigateToDestination(BlockListScreenDestination, viewModelScope)
    }

    fun openPrivateKeyResetDialog() {
        navigateToDestination(PrivateKeyResetDialogScreenDestination, viewModelScope)
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
                        _deleteAccountError.value = res.message.asString(getApplication())
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

    private suspend fun logoutAndClearSession() {
        clearLocalSessionUseCase.execute()
        navigateWithPopBackStackToDestination(
            LoginScreenDestination,
            viewModelScope,
            PopBackStackLevel.ALL
        )
    }

    fun navigateUp() {
        navigateUp(viewModelScope)
    }
}
