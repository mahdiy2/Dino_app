package com.dino.message.threadfeature.domain.usecase

import com.dino.message.corefeature.presentation.util.Resource
import com.dino.message.threadfeature.domain.model.PlaylistTrackMutation
import com.dino.message.threadfeature.domain.repository.ThreadRepository
import kotlinx.coroutines.flow.flow

class AddTrackToPlaylistUseCase constructor(
    private val threadRepository: ThreadRepository
) {
    fun execute(mediaId: Long) = flow<Resource<PlaylistTrackMutation>> {
        emit(Resource.Loading())
        emit(threadRepository.addTrackToPlaylist(mediaId = mediaId))
    }
}
