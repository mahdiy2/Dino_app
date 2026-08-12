package com.dino.message.threadfeature.domain.usecase

import com.dino.message.corefeature.presentation.util.Resource
import com.dino.message.threadfeature.domain.repository.ThreadRepository
import kotlinx.coroutines.flow.flow

class IsTrackInPlaylistUseCase constructor(
    private val threadRepository: ThreadRepository
) {
    fun execute(trackUrl: String) = flow<Resource<Boolean>> {
        emit(Resource.Loading())
        emit(threadRepository.isTrackInPlaylist(trackUrl = trackUrl))
    }
}
