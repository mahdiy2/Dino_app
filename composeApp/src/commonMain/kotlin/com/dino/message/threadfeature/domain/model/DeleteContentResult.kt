package com.dino.message.threadfeature.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class DeleteContentResult(
    val message: String?
)
