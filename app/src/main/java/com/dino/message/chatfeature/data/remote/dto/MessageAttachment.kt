package com.dino.message.chatfeature.data.remote.dto

 import kotlinx.serialization.SerialName
 import kotlinx.serialization.Serializable
 
 @Serializable
 data class MessageAttachment(
    val type: String,
    val url: String,
     @SerialName("mime_type") val mimeType: String,
     @SerialName("file_name") val fileName: String? = null,
     @SerialName("size_bytes") val sizeBytes: Long? = null,
     @SerialName("object_name") val objectName: String? = null
)
