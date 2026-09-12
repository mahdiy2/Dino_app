package com.dino.message.corefeature.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class ImageDialogData(
    val titleStrRes: Int = 0,
    val resId: Int = -1,
    val uriString: String? = null,
    val url: String? = null,
    val type: ImageDialogImageResourceType = ImageDialogImageResourceType.ResId
)
