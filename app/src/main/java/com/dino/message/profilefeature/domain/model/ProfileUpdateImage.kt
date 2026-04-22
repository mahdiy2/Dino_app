package com.dino.message.profilefeature.domain.model

import android.net.Uri

data class ProfileUpdateImage(
    val uri: Uri,
    val fileName: String,
    val mimeType: String,
    val sizeBytes: Long? = null,
)
