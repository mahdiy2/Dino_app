package com.dino.message.chatfeature.domain.model

data class MessageAttachment(
    val type: String,
    val url: String? = null,
    val mimeType: String? = null,
    val fileName: String? = null,
    val sizeBytes: Long? = null,
    val objectName: String? = null,
    val storyId: Long? = null,
    val storyLink: String? = null,
    val storyMediaType: String? = null,
    val storyMediaUrl: String? = null,
    val storyReferencePreview: StoryReferencePreview? = null
)

data class StoryReferencePreview(
    val storyId: Long? = null,
    val mediaType: String? = null,
    val mediaUrl: String? = null
)
