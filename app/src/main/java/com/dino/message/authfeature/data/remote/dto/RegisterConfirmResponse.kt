package com.dino.message.authfeature.data.remote.dto

import com.dino.message.authfeature.domain.model.RegisterConfirmUser
import com.dino.message.corefeature.data.remote.ResponseToResultMapper
import com.dino.message.corefeature.data.remote.dto.BaseResponse
import kotlinx.serialization.Serializable

@Serializable
data class RegisterConfirmResponse(
    override val message: String,
) : BaseResponse(), ResponseToResultMapper<RegisterConfirmUser> {

    override fun mapResponseToResult(): RegisterConfirmUser {
        return if (message.contains("registered", ignoreCase = true)) {
            RegisterConfirmUser()
        } else {
            throw Exception("Error in registration confirmation")
        }
    }
}
