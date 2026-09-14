package com.dino.message.corefeature.presentation.activity

import com.dino.message.corefeature.domain.model.AppLanguage
import com.dino.message.corefeature.domain.model.toBoolean
import com.dino.message.corefeature.domain.settings.AppSettingsStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class MainActivityUiSettingsState(
    private val appSettingsStore: AppSettingsStore,
) {
    val appLanguage: Flow<AppLanguage> = appSettingsStore.getLanguage()

    fun darkTheme(systemSettings: Boolean): Flow<Boolean> {
        return appSettingsStore.getTheme().map { it.toBoolean(systemSettings) }
    }
}
