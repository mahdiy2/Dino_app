package com.dino.message.corefeature.data.upload

import com.dino.message.threadfeature.domain.model.CreatePostMedia
import kotlinx.serialization.Serializable

@Serializable
data class StagedMedia(
    val displayName: String,
    val mimeType: String,
    val sizeBytes: Long,
    val durationMs: Long?,
    val trackTitle: String? = null,
    val trackArtist: String? = null,
)

@Serializable
sealed interface MediaSubmissionCommand {
    @Serializable
    data class PrivateAttachment(val recipient: String, val preparedMessage: String) : MediaSubmissionCommand

    @Serializable
    data class GroupAttachment(val groupId: Int, val preparedMessage: String) : MediaSubmissionCommand

    @Serializable
    data class Profile(
        val name: String?, val bio: String?, val shape: String?,
        val hasImage: Boolean, val hasVideo: Boolean,
        val removeImage: Boolean, val removeVideo: Boolean,
    ) : MediaSubmissionCommand

    @Serializable
    data class Post(
        val postId: Long?, val text: String, val followersOnly: Boolean?,
        val quotedPostId: Long?, val keepMediaIds: List<Long>, val music: Boolean,
    ) : MediaSubmissionCommand

    @Serializable
    data class Story(val mentions: List<String>) : MediaSubmissionCommand
}

@Serializable
data class StoredMediaSubmission(val command: MediaSubmissionCommand, val files: List<StagedMedia>)

internal fun stagedMetadata(media: CreatePostMedia, bytes: Long) = StagedMedia(
    media.fileName, media.mimeType, bytes, media.file.metadata?.durationMs, media.trackTitle, media.trackArtist,
)
