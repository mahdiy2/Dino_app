package com.dino.message.chatfeature.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MessageAttachment(
    val type: String,
    val url: String? = null,
    @SerialName("mime_type") val mimeType: String? = null,
    @SerialName("file_name") val fileName: String? = null,
    @SerialName("size_bytes") val sizeBytes: Long? = null,
    @SerialName("object_name") val objectName: String? = null,
    @SerialName("story_id") val storyId: Long? = null,
    @SerialName("story_link") val storyLink: String? = null,
    @SerialName("story_media_type") val storyMediaType: String? = null,
    @SerialName("story_media_url") val storyMediaUrl: String? = null,
    @SerialName("story_reference_preview") val storyReferencePreview: StoryReferencePreview? = null
)

@Serializable
data class StoryReferencePreview(
    @SerialName("story_id") val storyId: Long? = null,
    @SerialName("media_type") val mediaType: String? = null,
    @SerialName("media_url") val mediaUrl: String? = null
)
