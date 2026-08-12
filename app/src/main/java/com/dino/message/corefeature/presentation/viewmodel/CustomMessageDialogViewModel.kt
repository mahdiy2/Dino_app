package com.dino.message.corefeature.presentation.viewmodel

import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.dino.message.DinoOrderApplication


/**
 * ViewModel class for the custom message dialog.
 * Extends [AndroidViewModel] to have access to the application context.
 * Implements [HaveUIEvent] interface to handle UI events.
 *
 * @param app The application context.
 */
class CustomMessageDialogViewModel constructor(
    app: Context,
) : AndroidViewModel(app as DinoOrderApplication),
    HaveUIEvent by HaveUIEventImpl(app as DinoOrderApplication) {

    /**
     * Navigates up in the navigation hierarchy.
     */
    fun navigateUp() {
        navigateUp(viewModelScope)
    }
}
