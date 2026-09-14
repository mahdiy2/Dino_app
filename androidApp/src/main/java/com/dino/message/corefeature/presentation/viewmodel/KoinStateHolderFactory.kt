package com.dino.message.corefeature.presentation.viewmodel

import kotlin.reflect.KClass
import org.koin.core.context.GlobalContext
import org.koin.core.parameter.parametersOf

class KoinStateHolderFactory : StateHolderFactory {
    override fun <T : Any> create(
        holderClass: KClass<T>,
        parameters: List<Any?>,
    ): T {
        return GlobalContext.get().get(
            clazz = holderClass,
            parameters = {
                parametersOf(*parameters.toTypedArray())
            },
        )
    }
}
