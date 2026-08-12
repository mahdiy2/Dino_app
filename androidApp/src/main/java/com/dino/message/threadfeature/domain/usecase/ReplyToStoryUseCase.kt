package com.dino.message.threadfeature.domain.usecase

import com.dino.message.corefeature.domain.result.Resource
import com.dino.message.threadfeature.domain.model.StoryReplyResult
import com.dino.message.threadfeature.domain.repository.ThreadRepository
import kotlinx.coroutines.flow.flow

class ReplyToStoryUseCase constructor(
    private val threadRepository: ThreadRepository
) {
    fun execute(storyId: Long, replyText: String) = flow<Resource<StoryReplyResult>> {
        emit(Resource.Loading())
        emit(threadRepository.replyToStory(storyId, replyText))
    }
}
