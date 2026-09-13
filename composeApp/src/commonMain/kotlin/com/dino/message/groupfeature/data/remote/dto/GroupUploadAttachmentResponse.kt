package com.dino.message.groupfeature.data.remote.dto

import com.dino.message.chatfeature.data.remote.dto.MessageAttachment
import com.dino.message.corefeature.data.remote.ResponseToResultMapper
import kotlinx.serialization.Serializable

@Serializable
data class GroupUploadAttachmentResponse(
    val attachment: MessageAttachment,
) : ResponseToResultMapper<MessageAttachment> {
    override fun mapResponseToResult(): MessageAttachment = attachment
}
