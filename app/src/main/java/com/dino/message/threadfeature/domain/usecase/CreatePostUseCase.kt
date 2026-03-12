package com.dino.message.threadfeature.domain.usecase

import com.dino.message.corefeature.presentation.util.Resource
import com.dino.message.threadfeature.domain.model.CreatedPost
import com.dino.message.threadfeature.domain.model.CreatePostMedia
import com.dino.message.threadfeature.domain.repository.ThreadRepository
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class CreatePostUseCase @Inject constructor(
    private val threadRepository: ThreadRepository
) {
    fun execute(text: String, mediaFiles: List<CreatePostMedia> = emptyList()) = flow<Resource<CreatedPost>> {
        emit(Resource.Loading())
        emit(threadRepository.createPost(text = text, mediaFiles = mediaFiles))
    }
}
