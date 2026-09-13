package com.dino.message.spashscreen.viewmodel

import com.dino.message.corefeature.domain.model.StringResourceContent

data class SplashUiState(
    val isChecking: Boolean = true,
    val errorMessage: StringResourceContent? = null,
    val showLanguageSelection: Boolean = false,
    val isSavingLanguage: Boolean = false,
)
