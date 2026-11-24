package com.dino.message.threadfeature.domain.usecase

import com.dino.message.corefeature.domain.result.Resource
import com.dino.message.threadfeature.domain.model.CreatedPost
import com.dino.message.threadfeature.domain.model.CreatePostMedia
import com.dino.message.threadfeature.domain.repository.ThreadRepository
import kotlinx.coroutines.flow.flow

class CreatePostUseCase constructor(
    private val threadRepository: ThreadRepository
) {
    fun execute(
        text: String,
        mediaFiles: List<CreatePostMedia> = emptyList(),
        followersOnly: Boolean = false,
        quotedPostId: Long? = null
    ) = flow<Resource<CreatedPost>> {
        emit(Resource.Loading())
        emit(
            threadRepository.createPost(
                text = text,
                mediaFiles = mediaFiles,
                followersOnly = followersOnly,
                quotedPostId = quotedPostId
            )
        )
    }
}
