package com.dino.message.threadfeature.data.remote.dto

import com.dino.message.corefeature.data.remote.ResponseToResultMapper
import com.dino.message.threadfeature.domain.model.PlaylistPage
import com.dino.message.threadfeature.domain.model.PlaylistTrack
import kotlinx.serialization.Serializable

@Serializable
data class GetPlaylistTracksResponse(
    val page: Int,
    val limit: Int,
    val total: Int,
    val tracks: List<PlaylistTrack>
) : ResponseToResultMapper<PlaylistPage> {
    override fun mapResponseToResult(): PlaylistPage = PlaylistPage(
        page = page,
        limit = limit,
        total = total,
        tracks = tracks
    )
}
