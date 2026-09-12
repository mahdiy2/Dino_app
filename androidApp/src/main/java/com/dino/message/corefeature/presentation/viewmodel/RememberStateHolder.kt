package com.dino.message.corefeature.presentation.viewmodel

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import org.koin.core.context.GlobalContext
import org.koin.core.parameter.ParametersDefinition

@Composable
inline fun <reified T : Any> rememberStateHolder(
    vararg keys: Any?,
    noinline parameters: ParametersDefinition? = null,
): T {
    val holder = remember(*keys) {
        GlobalContext.get().get<T>(parameters = parameters)
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
