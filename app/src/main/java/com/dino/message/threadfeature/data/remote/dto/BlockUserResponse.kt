package com.dino.message.threadfeature.data.remote.dto

import com.dino.message.corefeature.data.remote.ResponseToResultMapper
import com.dino.message.corefeature.data.remote.dto.BaseResponse
import com.dino.message.threadfeature.domain.model.BlockedUserResult
import kotlinx.serialization.Serializable

@Serializable
data class BlockUserResponse(
    val blocked_username: String,
    val created: Boolean,
    override val message: String
) : BaseResponse(), ResponseToResultMapper<BlockedUserResult> {
    override fun mapResponseToResult(): BlockedUserResult = BlockedUserResult(
        message = message,
        blockedUsername = blocked_username,
        created = created
    )
}
