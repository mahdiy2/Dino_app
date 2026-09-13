package com.dino.message.threadfeature.data.remote.dto

import com.dino.message.corefeature.data.remote.ResponseToResultMapper
import com.dino.message.corefeature.data.remote.dto.BaseResponse
import com.dino.message.threadfeature.domain.model.DeleteContentResult
import kotlinx.serialization.Serializable

@Serializable
data class DeleteContentResponse(
    override val message: String
) : BaseResponse(), ResponseToResultMapper<DeleteContentResult> {
    override fun mapResponseToResult(): DeleteContentResult = DeleteContentResult(message = message)
}
