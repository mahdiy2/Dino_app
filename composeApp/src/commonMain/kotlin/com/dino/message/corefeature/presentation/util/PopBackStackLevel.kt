package com.dino.message.corefeature.presentation.util

import kotlinx.serialization.Serializable

@Serializable
sealed class PopBackStackLevel {
    data object ALL : PopBackStackLevel()

    @Serializable
    data class Number(val number: Int) : PopBackStackLevel()
}
