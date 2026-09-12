package com.dino.message.profilefeature.presentation.viewmodel

import com.dino.message.corefeature.presentation.util.asString
import com.dino.message.corefeature.domain.result.Resource
import com.dino.message.corefeature.domain.resource.StringResolver
import com.dino.message.corefeature.presentation.viewmodel.HaveUIEvent
import com.dino.message.corefeature.presentation.viewmodel.HaveUIEventImpl
import com.dino.message.corefeature.presentation.util.UiEventBus
import com.dino.message.corefeature.presentation.viewmodel.ScopedStateHolder
import com.dino.message.profilefeature.domain.model.AboutUs
import com.dino.message.profilefeature.domain.usecase.GetAboutUsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.dino.message.corefeature.presentation.navigation.AppRoute

class AboutUsViewModel constructor(
    private val stringResolver: StringResolver,
    uiEventBus: UiEventBus,
    private val getAboutUsUseCase: GetAboutUsUseCase,
) : ScopedStateHolder(),
    HaveUIEvent by HaveUIEventImpl(uiEventBus) {

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
                    _error.value = res.message.resolveWith(stringResolver)
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
