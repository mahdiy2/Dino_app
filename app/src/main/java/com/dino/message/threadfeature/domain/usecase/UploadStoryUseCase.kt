package com.dino.message.threadfeature.domain.usecase

import com.dino.message.corefeature.presentation.util.Resource
import com.dino.message.threadfeature.domain.model.CreatePostMedia
import com.dino.message.threadfeature.domain.model.StoryUploadResult
import com.dino.message.threadfeature.domain.repository.ThreadRepository
import kotlinx.coroutines.flow.flow

class UploadStoryUseCase constructor(
    private val threadRepository: ThreadRepository
) {
    fun execute(mediaFile: CreatePostMedia, mentions: List<String>) = flow<Resource<StoryUploadResult>> {
        emit(Resource.Loading())
        emit(threadRepository.uploadStory(mediaFile, mentions))
    }
}
