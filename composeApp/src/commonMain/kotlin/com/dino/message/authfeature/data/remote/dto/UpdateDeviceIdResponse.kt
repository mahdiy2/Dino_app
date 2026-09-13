package com.dino.message.authfeature.data.remote.dto

import com.dino.message.corefeature.data.remote.ResponseToResultMapper
import com.dino.message.corefeature.data.remote.dto.BaseResponse
import com.dino.message.profilefeature.domain.model.ProfileFollowResult
import kotlinx.serialization.Serializable

@Serializable
data class UpdateDeviceIdResponse(
    override val message: String,
) : BaseResponse(), ResponseToResultMapper<ProfileFollowResult> {
    override fun mapResponseToResult(): ProfileFollowResult {
        return ProfileFollowResult(message = message)
    }
}
