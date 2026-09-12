package com.dino.message.corefeature.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class CustomMessageDialogContentItem(
    var title: StringResourceContent,
    val descriptions: MutableList<StringResourceContent>,
)
