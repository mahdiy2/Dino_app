package com.dino.message.profilefeature.presentation.viewmodel

import com.dino.message.corefeature.presentation.util.asString
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.dino.message.DinoOrderApplication
import com.dino.message.corefeature.domain.result.Resource
import com.dino.message.corefeature.presentation.viewmodel.HaveUIEvent
import com.dino.message.corefeature.presentation.viewmodel.HaveUIEventImpl
import com.dino.message.profilefeature.domain.model.AboutUs
import com.dino.message.profilefeature.domain.usecase.GetAboutUsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.dino.message.corefeature.presentation.navigation.AppRoute

class AboutUsViewModel constructor(
    app: Context,
    private val getAboutUsUseCase: GetAboutUsUseCase,
) : AndroidViewModel(app as DinoOrderApplication),
    HaveUIEvent by HaveUIEventImpl(app as DinoOrderApplication) {

    private val _aboutUs = MutableStateFlow<AboutUs?>(null)
    val aboutUs: StateFlow<AboutUs?> = _aboutUs.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun refresh() = viewModelScope.launch {
        getAboutUsUseCase.execute().collect { res ->
            when (res) {
                is Resource.Loading -> {
                    _isLoading.value = true
                    _error.value = null
                }

                is Resource.Success -> {
                    _aboutUs.value = res.data
                    _isLoading.value = false
                    _error.value = null
                }

                is Resource.Error -> {
                    _isLoading.value = false
                    _error.value = res.message.asString(getApplication())
                }
            }
        }
    }

    fun navigateUp() {
        navigateUp(viewModelScope)
    }

    fun openUserProfile(username: String) {
        if (username.isBlank()) return
        navigateToDestination(AppRoute.Profile(username = username), viewModelScope)
    }
}
