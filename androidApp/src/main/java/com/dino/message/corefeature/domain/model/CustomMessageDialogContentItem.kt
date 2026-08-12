package com.dino.message.corefeature.domain.model

import kotlinx.serialization.Serializable

/**
 * The `CustomMessageDialogMessage` data class represents a custom message dialog message.
 *
 * @param title The title of the dialog message.
 * @param descriptions The descriptions of the dialog message.
 */
@Serializable
data class CustomMessageDialogContentItem(
    var title: StringResourceContent,
    val descriptions: MutableList<StringResourceContent>,
)
