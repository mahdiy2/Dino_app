package com.dino.message.corefeature.presentation.viewmodel

import com.dino.message.corefeature.domain.model.AppLanguage
import com.dino.message.corefeature.presentation.activity.MainActivityUiSettingsState
import kotlinx.coroutines.flow.Flow


class MainActivityViewModel(
    private val uiSettingsState: MainActivityUiSettingsState,
) : ScopedStateHolder() {

    /**
     * Flow representing the current app language obtained from the data store.
     */
    val appLanguage: Flow<AppLanguage> = uiSettingsState.appLanguage

    /**
     * Retrieves the system theme based on the given `systemSettings` flag.
     *
     * @param systemSettings A flag indicating whether the system settings should be used.
     * @return A [Flow] emitting the theme value.
     */
    fun getSystemTheme(systemSettings: Boolean): Flow<Boolean> =
        uiSettingsState.darkTheme(systemSettings)
}
