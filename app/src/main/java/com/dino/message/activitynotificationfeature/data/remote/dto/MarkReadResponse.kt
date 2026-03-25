package com.dino.message.activitynotificationfeature.data.remote.dto

import com.dino.message.activitynotificationfeature.domain.model.MarkReadResult
import com.dino.message.corefeature.data.remote.ResponseToResultMapper
import kotlinx.serialization.Serializable

@Serializable
data class MarkReadResponse(
    val updated: Int
) : ResponseToResultMapper<MarkReadResult> {
    override fun mapResponseToResult(): MarkReadResult =
        MarkReadResult(updated = updated)
}
