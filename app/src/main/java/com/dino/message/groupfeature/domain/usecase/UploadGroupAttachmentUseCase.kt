package com.dino.message.groupfeature.domain.usecase

import android.net.Uri
import com.dino.message.chatfeature.data.remote.dto.MessageAttachment
import com.dino.message.corefeature.presentation.util.Resource
import com.dino.message.groupfeature.domain.repository.GroupRepository
import kotlinx.coroutines.flow.flow

class UploadGroupAttachmentUseCase constructor(
    private val repository: GroupRepository,
) {
    fun execute(groupId: Int, fileUri: Uri) = flow<Resource<MessageAttachment>> {
        emit(Resource.Loading())
        emit(repository.uploadGroupAttachment(groupId, fileUri))
    }
}
