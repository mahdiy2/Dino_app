package com.dino.message.profilefeature.data.remote.dto

import com.dino.message.corefeature.data.remote.ResponseToResultMapper
import com.dino.message.corefeature.data.remote.dto.BaseResponse
import com.dino.message.profilefeature.domain.model.ProfileFollowResult
import kotlinx.serialization.Serializable

@Serializable
data class ProfileFollowResponse(
    override val message: String
) : BaseResponse(), ResponseToResultMapper<ProfileFollowResult> {
    override fun mapResponseToResult(): ProfileFollowResult = ProfileFollowResult(message = message)
}
