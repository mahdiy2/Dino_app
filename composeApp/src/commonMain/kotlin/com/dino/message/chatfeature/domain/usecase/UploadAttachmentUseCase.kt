package com.dino.message.chatfeature.domain.usecase

import com.dino.message.chatfeature.domain.repository.ChatRepository
import com.dino.message.corefeature.domain.upload.ReadableMediaFile
import com.dino.message.corefeature.domain.result.Resource
import kotlinx.coroutines.flow.flow

class UploadAttachmentUseCase constructor(
    private val chatRepository: ChatRepository
) {
    fun execute(file: ReadableMediaFile) = flow {
        emit(Resource.Loading())
        emit(chatRepository.uploadAttachment(file))
    }
}
