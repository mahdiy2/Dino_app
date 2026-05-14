package com.dino.message.threadfeature.domain.usecase

import com.dino.message.corefeature.presentation.util.Resource
import com.dino.message.threadfeature.domain.model.CreatePostMedia
import com.dino.message.threadfeature.domain.model.DeleteContentResult
import com.dino.message.threadfeature.domain.repository.ThreadRepository
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class EditPostUseCase @Inject constructor(
    private val repository: ThreadRepository
) {
    fun execute(
        postId: Long,
        text: String,
        mediaFiles: List<CreatePostMedia> = emptyList(),
        keepMediaIds: List<Long> = emptyList(),
        followersOnly: Boolean? = null
    ) = flow<Resource<DeleteContentResult>> {
        emit(Resource.Loading())
        emit(
            repository.editPost(
                postId = postId,
                text = text,
                mediaFiles = mediaFiles,
                keepMediaIds = keepMediaIds,
                followersOnly = followersOnly
            )
        )
    }
}
