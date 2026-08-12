package com.dino.message.corefeature.data.coroutine

import com.dino.message.corefeature.domain.coroutine.AppDispatchers
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

object AndroidAppDispatchers : AppDispatchers {
    override val io: CoroutineDispatcher = Dispatchers.IO
}
