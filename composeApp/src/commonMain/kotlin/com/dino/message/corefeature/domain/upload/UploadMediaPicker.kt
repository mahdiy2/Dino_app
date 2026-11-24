package com.dino.message.corefeature.domain.upload

enum class UploadMediaCategory(val mimePrefix: String) {
    Image("image"),
    Video("video"),
    Audio("audio"),
}

/** Call from the UI thread. Successful null results indicate user cancellation. */
fun interface UploadMediaPicker {
    fun pick(
        categories: Set<UploadMediaCategory>,
        onResult: (Result<ReadableMediaFile?>) -> Unit,
    )
}
