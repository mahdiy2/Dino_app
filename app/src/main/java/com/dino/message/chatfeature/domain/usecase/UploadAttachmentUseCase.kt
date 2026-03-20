package com.dino.message.chatfeature.domain.usecase

import android.net.Uri
import com.dino.message.chatfeature.domain.repository.ChatRepository
import com.dino.message.corefeature.presentation.util.Resource
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class UploadAttachmentUseCase @Inject constructor(
    private val chatRepository: ChatRepository
) {
    fun execute(fileUri: Uri) = flow {
        emit(Resource.Loading())
        emit(chatRepository.uploadAttachment(fileUri))
    }
}
