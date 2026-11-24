package com.dino.message.threadfeature.domain.model

import com.dino.message.corefeature.domain.model.AppStringKey
import com.dino.message.corefeature.domain.model.StringResourceContent
import com.dino.message.corefeature.domain.model.asStringResourceContent
import com.dino.message.corefeature.domain.upload.MediaMimePolicy
import com.dino.message.corefeature.domain.upload.UploadMediaCategory

private const val MAX_STORY_SECONDS = 30
private val storyCategories = setOf(UploadMediaCategory.Image, UploadMediaCategory.Video)

fun validateStoryUpload(
    media: CreatePostMedia,
    durationMs: Long? = media.file.metadata?.durationMs,
): StringResourceContent? {
    MediaMimePolicy.validateUploadMimeType(media.mimeType, storyCategories)?.let { return it }
    return if (MediaMimePolicy.mediaCategory(media.mimeType) == "video" &&
        durationMs != null && durationMs > MAX_STORY_SECONDS * 1_000L
    ) {
        AppStringKey.StoryVideoDurationLimit.asStringResourceContent(MAX_STORY_SECONDS)
    } else {
        null
    }
}
