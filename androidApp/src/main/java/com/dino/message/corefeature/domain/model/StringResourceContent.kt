package com.dino.message.corefeature.domain.model

import com.dino.message.corefeature.domain.resource.StringResolver
import kotlinx.serialization.Serializable


@Serializable
sealed class StringResourceContent {
    data class Str(val data: String) : StringResourceContent()
    data class StringResId(val data: Int) : StringResourceContent()

    fun resolveWith(stringResolver: StringResolver): String {
        return when (this) {
            is Str -> stringResolver.resolve(this.data)
            is StringResId -> stringResolver.resolveStringId(this.data)
        }
    }
}

fun Int.asStringResourceContent() = StringResourceContent.StringResId(this)
fun String.asStringResourceContent() = StringResourceContent.Str(this)
