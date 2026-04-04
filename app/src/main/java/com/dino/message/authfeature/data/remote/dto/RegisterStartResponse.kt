package com.dino.message.authfeature.data.remote.dto

import com.dino.message.authfeature.domain.model.RegisterStartUser
import com.dino.message.corefeature.data.remote.ResponseToResultMapper
import com.dino.message.corefeature.data.remote.dto.BaseResponse
import kotlinx.serialization.Serializable

@Serializable
data class RegisterStartResponse(
    override val message: String,
    val registration_id: String,
    val expires_in_seconds: Int,
) : BaseResponse(), ResponseToResultMapper<RegisterStartUser> {

    override fun mapResponseToResult(): RegisterStartUser {
        if (registration_id.isBlank()) throw Exception("Missing registration id")
        return RegisterStartUser(
            registrationId = registration_id,
            expiresInSeconds = expires_in_seconds
        )
    }
}
