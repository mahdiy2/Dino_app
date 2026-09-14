package com.dino.message.corefeature.presentation.viewmodel

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import kotlin.reflect.KClass

interface StateHolderFactory {
    fun <T : Any> create(
        holderClass: KClass<T>,
        parameters: List<Any?> = emptyList(),
    ): T
}

val LocalStateHolderFactory = staticCompositionLocalOf<StateHolderFactory> {
    error("StateHolderFactory is not provided")
}

@Composable
fun <T : Any> rememberStateHolder(
    holderClass: KClass<T>,
    vararg keys: Any?,
    parameters: List<Any?> = emptyList(),
): T {
    val factory = LocalStateHolderFactory.current
    val holder = remember(factory, *keys) {
        factory.create(holderClass, parameters)
    }
    DisposableEffect(holder) {
        onDispose {
            if (holder is StateHolder) {
                holder.clear()
            }
        }
    }
    return holder
}
