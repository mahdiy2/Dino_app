package com.dino.message.corefeature.presentation.viewmodel

class CustomMessageDialogViewModel(
    uiEventBus: com.dino.message.corefeature.presentation.util.UiEventBus,
) : ScopedStateHolder(),
    HaveUIEvent by HaveUIEventImpl(uiEventBus) {

    fun navigateUp() {
        navigateUp(scope)
    }
}
