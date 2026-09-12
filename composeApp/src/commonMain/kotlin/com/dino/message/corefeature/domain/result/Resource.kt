package com.dino.message.corefeature.domain.result

import com.dino.message.corefeature.domain.model.StringResourceContent

sealed class Resource<out R> {
    class Success<out T>(val data: T) : Resource<T>()

    class Error<out T>(
        val message: StringResourceContent,
        val data: T? = null
    ) : Resource<T>()

    class Loading<out T>(val data: T? = null) : Resource<T>()
}
