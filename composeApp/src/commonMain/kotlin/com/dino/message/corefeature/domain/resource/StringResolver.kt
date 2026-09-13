package com.dino.message.corefeature.domain.resource

import com.dino.message.corefeature.domain.model.AppStringKey

interface StringResolver {
    fun resolve(value: String): String = value
    fun resolve(key: AppStringKey, vararg args: Any): String
}
