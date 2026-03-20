package com.dino.message.chatfeature.data.remote.dto

import com.dino.message.corefeature.data.remote.ResponseToResultMapper
import kotlinx.serialization.Serializable

@Serializable
data class UploadAttachmentResponse(
    val attachment: MessageAttachment
) : ResponseToResultMapper<MessageAttachment> {
    override fun mapResponseToResult(): MessageAttachment = attachment
}
