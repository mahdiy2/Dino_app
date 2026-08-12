package com.dino.message.threadfeature.domain.usecase

import com.dino.message.corefeature.domain.result.Resource
import com.dino.message.threadfeature.domain.model.DeleteContentResult
import com.dino.message.threadfeature.domain.repository.ThreadRepository
import kotlinx.coroutines.flow.flow

class RemoveTrackFromPlaylistUseCase constructor(
    private val threadRepository: ThreadRepository
) {
    fun execute(trackId: Long) = flow<Resource<DeleteContentResult>> {
        emit(Resource.Loading())
        emit(threadRepository.removeTrackFromPlaylist(trackId = trackId))
    }
}
