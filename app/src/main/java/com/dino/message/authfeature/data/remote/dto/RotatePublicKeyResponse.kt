package com.dino.message.authfeature.data.remote.dto

import com.dino.message.authfeature.domain.model.RotatePublicKeyUser
import com.dino.message.corefeature.data.remote.ResponseToResultMapper
import com.dino.message.corefeature.data.remote.dto.BaseResponse
import kotlinx.serialization.Serializable

@Serializable
data class RotatePublicKeyResponse(
    override val message: String,
) : BaseResponse(), ResponseToResultMapper<RotatePublicKeyUser> {

    override fun mapResponseToResult(): RotatePublicKeyUser {
        return if (message.contains("updated", ignoreCase = true)) {
            RotatePublicKeyUser()
        } else {
            throw Exception("Error rotating key")
        }
    }
}
