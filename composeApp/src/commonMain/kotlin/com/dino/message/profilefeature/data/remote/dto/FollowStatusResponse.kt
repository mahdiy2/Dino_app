package com.dino.message.profilefeature.data.remote.dto

import com.dino.message.corefeature.data.remote.ResponseToResultMapper
import kotlinx.serialization.Serializable

@Serializable
data class FollowStatusResponse(
    val is_following: Boolean
) : ResponseToResultMapper<Boolean> {
    override fun mapResponseToResult(): Boolean = is_following
}
