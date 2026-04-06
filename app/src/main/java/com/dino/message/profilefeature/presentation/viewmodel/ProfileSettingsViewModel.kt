package com.dino.message.profilefeature.presentation.viewmodel

import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.dino.message.DinoOrderApplication
import com.dino.message.corefeature.presentation.viewmodel.HaveUIEvent
import com.dino.message.corefeature.presentation.viewmodel.HaveUIEventImpl
import com.ramcosta.composedestinations.generated.destinations.BlockListScreenDestination
import com.ramcosta.composedestinations.generated.destinations.PrivateKeyResetDialogScreenDestination
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

@HiltViewModel
class ProfileSettingsViewModel @Inject constructor(
    @ApplicationContext app: Context
) : AndroidViewModel(app as DinoOrderApplication),
    HaveUIEvent by HaveUIEventImpl(app as DinoOrderApplication) {

    fun openBlockList() {
        navigateToDestination(BlockListScreenDestination, viewModelScope)
    }

    fun openPrivateKeyResetDialog() {
        navigateToDestination(PrivateKeyResetDialogScreenDestination, viewModelScope)
    }

    fun navigateUp() {
        navigateUp(viewModelScope)
    }
}
