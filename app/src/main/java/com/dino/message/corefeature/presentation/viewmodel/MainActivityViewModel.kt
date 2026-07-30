package com.dino.message.corefeature.presentation.viewmodel

import android.content.Context
import androidx.lifecycle.AndroidViewModel
import com.dino.message.DinoOrderApplication
import com.dino.message.corefeature.data.spref.DataStoreUtil
import com.dino.message.corefeature.domain.model.AppLanguage
import com.dino.message.corefeature.domain.model.toBoolean
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map


class MainActivityViewModel constructor(
    private val dataStoreUtil: DataStoreUtil,
    private val app: Context

) : AndroidViewModel(app as DinoOrderApplication),
    HaveUIEvent by HaveUIEventImpl(app as DinoOrderApplication){

    /**
     * Flow representing the current app language obtained from the data store.
     */
    val appLanguage: Flow<AppLanguage> = dataStoreUtil.getLanguage()

    /**
     * Retrieves the system theme based on the given `systemSettings` flag.
     *
     * @param systemSettings A flag indicating whether the system settings should be used.
     * @return A [Flow] emitting the theme value.
     */
    fun getSystemTheme(systemSettings: Boolean): Flow<Boolean> =
        dataStoreUtil.getTheme().map { it.toBoolean(systemSettings) }
}