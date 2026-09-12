package com.dino.message.threadfeature.presentation.viewmodel

import com.dino.message.corefeature.presentation.viewmodel.HaveUIEvent
import com.dino.message.corefeature.presentation.viewmodel.HaveUIEventImpl
import com.dino.message.corefeature.presentation.util.UiEventBus
import com.dino.message.corefeature.presentation.viewmodel.ScopedStateHolder

class TrackPlaylistAddersDialogViewModel constructor(
    uiEventBus: UiEventBus,
) : ScopedStateHolder(),
    HaveUIEvent by HaveUIEventImpl(uiEventBus) {

    fun dismissDialog() {
        navigateUp(viewModelScope)
    }
}
