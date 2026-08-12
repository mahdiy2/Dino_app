package com.dino.message.threadfeature.presentation.viewmodel

import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.dino.message.DinoOrderApplication
import com.dino.message.corefeature.presentation.viewmodel.HaveUIEvent
import com.dino.message.corefeature.presentation.viewmodel.HaveUIEventImpl

class TrackPlaylistAddersDialogViewModel constructor(
    app: Context
) : AndroidViewModel(app as DinoOrderApplication),
    HaveUIEvent by HaveUIEventImpl(app as DinoOrderApplication) {

    fun dismissDialog() {
        navigateUp(viewModelScope)
    }
}
