package com.dino.message.corefeature.domain.model

import com.dino.message.corefeature.domain.resource.StringResolver
import kotlinx.serialization.Serializable

@Serializable
sealed class StringResourceContent {
    @Serializable
    data class Str(val data: String) : StringResourceContent()
    @Serializable
    data class Key(
        val key: AppStringKey,
        val args: List<StringResourceArg> = emptyList(),
    ) : StringResourceContent()

    fun resolveWith(stringResolver: StringResolver): String {
        return when (this) {
            is Str -> stringResolver.resolve(this.data)
            is Key -> stringResolver.resolve(this.key, *this.args.toFormatArgs())
        }
    }
}

fun String.asStringResourceContent() = StringResourceContent.Str(this)
fun AppStringKey.asStringResourceContent(vararg args: Any) =
    StringResourceContent.Key(this, args.map { it.toStringResourceArg() })

@Serializable
sealed class StringResourceArg {
    @Serializable
    data class Text(val value: String) : StringResourceArg()
    @Serializable
    data class IntValue(val value: Int) : StringResourceArg()
    @Serializable
    data class LongValue(val value: Long) : StringResourceArg()
    @Serializable
    data class DoubleValue(val value: Double) : StringResourceArg()
    @Serializable
    data class BooleanValue(val value: Boolean) : StringResourceArg()

    fun toFormatArg(): Any = when (this) {
        is Text -> value
        is IntValue -> value
        is LongValue -> value
        is DoubleValue -> value
        is BooleanValue -> value
    }
}

fun List<StringResourceArg>.toFormatArgs(): Array<Any> = map { it.toFormatArg() }.toTypedArray()

private fun Any.toStringResourceArg(): StringResourceArg = when (this) {
    is Int -> StringResourceArg.IntValue(this)
    is Long -> StringResourceArg.LongValue(this)
    is Double -> StringResourceArg.DoubleValue(this)
    is Float -> StringResourceArg.DoubleValue(this.toDouble())
    is Boolean -> StringResourceArg.BooleanValue(this)
    else -> StringResourceArg.Text(toString())
}
