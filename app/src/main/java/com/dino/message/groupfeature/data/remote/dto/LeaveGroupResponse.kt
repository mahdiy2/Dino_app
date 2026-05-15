package com.dino.message.groupfeature.data.remote.dto

import com.dino.message.corefeature.data.remote.ResponseToResultMapper
import kotlinx.serialization.Serializable

@Serializable
data class LeaveGroupResponse(
    val message: String = "",
) : ResponseToResultMapper<Unit> {
    override fun mapResponseToResult(): Unit = Unit
}
