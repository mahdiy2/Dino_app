package com.dino.message.corefeature.presentation.viewmodel

import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.CoroutineScope

interface StateHolder {
    fun clear()
}

abstract class ScopedStateHolder : StateHolder {
    val scope: CoroutineScope = MainScope()
    protected val viewModelScope: CoroutineScope = scope

    override fun clear() {
        scope.cancel()
    }
}
