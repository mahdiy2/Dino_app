package com.dino.message.threadfeature.data.remote.dto

import com.dino.message.corefeature.data.remote.ResponseToResultMapper
import com.dino.message.corefeature.data.remote.dto.BaseResponse
import com.dino.message.threadfeature.domain.model.PlaylistTrack
import com.dino.message.threadfeature.domain.model.PlaylistTrackMutation
import kotlinx.serialization.Serializable

@Serializable
data class AddPlaylistTrackResponse(
    override val message: String,
    val created: Boolean,
    val track: PlaylistTrack
) : BaseResponse(), ResponseToResultMapper<PlaylistTrackMutation> {
    override fun mapResponseToResult(): PlaylistTrackMutation = PlaylistTrackMutation(
        message = message,
        created = created,
        track = track
    )
}
