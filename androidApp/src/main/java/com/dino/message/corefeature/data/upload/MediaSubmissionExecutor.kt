package com.dino.message.corefeature.data.upload

import com.dino.message.chatfeature.data.remote.ChatApiService
import com.dino.message.corefeature.domain.result.Resource
import com.dino.message.groupfeature.data.remote.GroupApiService
import com.dino.message.profilefeature.data.remote.ProfileApiService
import com.dino.message.profilefeature.domain.model.ProfileUpdateImage
import com.dino.message.threadfeature.data.remote.ThreadApiService
import com.dino.message.threadfeature.domain.model.CreatePostMedia
import kotlinx.serialization.json.Json

fun interface MediaSubmissionExecutor {
    suspend fun submit(command: MediaSubmissionCommand, media: List<CreatePostMedia>): Resource<String>
}

class ApiMediaSubmissionExecutor(
    private val chat: ChatApiService,
    private val group: GroupApiService,
    private val profile: ProfileApiService,
    private val thread: ThreadApiService,
) : MediaSubmissionExecutor {
    override suspend fun submit(command: MediaSubmissionCommand, media: List<CreatePostMedia>): Resource<String> =
        when (command) {
            is MediaSubmissionCommand.PrivateAttachment ->
                encode(chat.uploadAttachment(media.single().file))
            is MediaSubmissionCommand.GroupAttachment ->
                encode(group.uploadGroupAttachment(command.groupId, media.single().file))
            is MediaSubmissionCommand.Profile -> encode(profile.updateMyProfile(
                name = command.name,
                bio = command.bio,
                profileImageShape = command.shape,
                profileImage = if (command.hasImage) ProfileUpdateImage(media.first().file) else null,
                profileVideo = if (command.hasVideo) ProfileUpdateImage(media.last().file) else null,
                removeProfileImage = command.removeImage,
                removeProfileVideo = command.removeVideo,
            ))
            is MediaSubmissionCommand.Post -> if (command.postId == null) {
                encode(thread.createPost(command.text, media, command.followersOnly ?: false, command.quotedPostId))
            } else {
                encode(thread.editPost(command.postId, command.text, media, command.keepMediaIds, command.followersOnly))
            }
            is MediaSubmissionCommand.Story -> encode(thread.uploadStory(media.single(), command.mentions))
        }

    private inline fun <reified T> encode(result: Resource<T>): Resource<String> = when (result) {
        is Resource.Success -> Resource.Success(Json.encodeToString(result.data))
        is Resource.Error -> Resource.Error(result.message)
        is Resource.Loading -> Resource.Loading()
    }
}
