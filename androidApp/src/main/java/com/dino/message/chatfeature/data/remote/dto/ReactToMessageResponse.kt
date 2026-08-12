package com.dino.message.chatfeature.data.remote.dto

import com.dino.message.corefeature.data.remote.ResponseToResultMapper
import kotlinx.serialization.Serializable

@Serializable
data class ReactToMessageResponse(
    val status: String = "ok",
) : ResponseToResultMapper<Unit> {
    override fun mapResponseToResult(): Unit = Unit
}
