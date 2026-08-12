package com.dino.message.corefeature.presentation.util

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.dino.message.R
import com.dino.message.corefeature.domain.model.StringResourceContent

fun StringResourceContent.asString(context: Context): String {
    return when (this) {
        is StringResourceContent.Str -> data
        is StringResourceContent.StringResId -> context.getString(data)
    }
}

@Composable
fun StringResourceContent.asString(): String {
    return when (this) {
        is StringResourceContent.Str -> data
        is StringResourceContent.StringResId -> stringResource(id = data)
    }
}

@Composable
fun List<StringResourceContent>.asString(
    separator: String = " " + stringResource(id = R.string.and) + " "
) = map { it.asString() }.joinToString(separator = separator)
