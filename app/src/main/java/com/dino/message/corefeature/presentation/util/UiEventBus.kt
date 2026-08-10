package com.dino.message.corefeature.presentation.util

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

class UiEventBus {
    val mutableUIEventFlow: MutableSharedFlow<UIEvent> = MutableSharedFlow()
    val uIEventFlow: SharedFlow<UIEvent> = mutableUIEventFlow
}
