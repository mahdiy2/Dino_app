package com.dino.message.threadfeature.domain.usecase

import com.dino.message.corefeature.presentation.util.Resource
import com.dino.message.threadfeature.domain.model.PlaylistPage
import com.dino.message.threadfeature.domain.repository.ThreadRepository
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class GetPlaylistTracksUseCase @Inject constructor(
    private val threadRepository: ThreadRepository
) {
    fun execute(page: Int, limit: Int) = flow<Resource<PlaylistPage>> {
        emit(Resource.Loading())
        emit(threadRepository.getPlaylistTracks(page = page, limit = limit))
    }
}
