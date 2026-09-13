package com.dino.message.threadfeature.data.remote.dto

import com.dino.message.corefeature.data.remote.ResponseToResultMapper
import kotlinx.serialization.Serializable

@Serializable
data class PlaylistTrackExistsResponse(
    val exists: Boolean
) : ResponseToResultMapper<Boolean> {
    override fun mapResponseToResult(): Boolean = exists
}
