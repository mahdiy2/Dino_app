package com.dino.message.corefeature.presentation.activity

import com.dino.message.corefeature.data.spref.DataStoreUtil
import com.dino.message.corefeature.domain.model.AppLanguage
import com.dino.message.corefeature.domain.model.toBoolean
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class MainActivityUiSettingsState(
    private val dataStoreUtil: DataStoreUtil,
) {
    val appLanguage: Flow<AppLanguage> = dataStoreUtil.getLanguage()

    fun darkTheme(systemSettings: Boolean): Flow<Boolean> {
        return dataStoreUtil.getTheme().map { it.toBoolean(systemSettings) }
    }
}
