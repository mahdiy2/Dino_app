package com.dino.message.mainpage.data.remote.dto

import com.dino.message.corefeature.data.remote.ResponseToResultMapper
import kotlinx.serialization.Serializable

@Serializable
data class ContactsDeltaResponse(
    val contacts: List<DetailedContact> = emptyList(),
) : ResponseToResultMapper<List<DetailedContact>> {
    override fun mapResponseToResult(): List<DetailedContact> = contacts
}
