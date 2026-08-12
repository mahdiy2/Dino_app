package com.dino.message.corefeature.domain.coroutine

import kotlinx.coroutines.CoroutineDispatcher

interface AppDispatchers {
    val io: CoroutineDispatcher
}
