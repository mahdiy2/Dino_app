package com.dino.message.groupfeature.domain.usecase

import com.dino.message.chatfeature.data.remote.dto.MessageAttachment
import com.dino.message.corefeature.data.upload.ReadableMediaFile
import com.dino.message.corefeature.presentation.util.Resource
import com.dino.message.groupfeature.domain.repository.GroupRepository
import kotlinx.coroutines.flow.flow

class UploadGroupAttachmentUseCase constructor(
    private val repository: GroupRepository,
) {
    fun execute(groupId: Int, file: ReadableMediaFile) = flow<Resource<MessageAttachment>> {
        emit(Resource.Loading())
        emit(repository.uploadGroupAttachment(groupId, file))
    }
}
