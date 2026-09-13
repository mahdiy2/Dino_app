package com.dino.message.authfeature.data.remote.dto

import com.dino.message.authfeature.domain.model.KeyStatusUser
import com.dino.message.corefeature.data.remote.ResponseToResultMapper
import com.dino.message.corefeature.data.remote.dto.BaseResponse
import kotlinx.serialization.Serializable

@Serializable
data class KeyStatusResponse(
    override val message: String? = null,
    val has_public_key: Boolean = false,
) : BaseResponse(), ResponseToResultMapper<KeyStatusUser> {

    override fun mapResponseToResult(): KeyStatusUser {
        return KeyStatusUser(
            hasServerPublicKey = has_public_key
        )
    }
}
