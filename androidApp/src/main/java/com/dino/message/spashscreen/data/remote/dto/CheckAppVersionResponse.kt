package com.dino.message.spashscreen.data.remote.dto

import com.dino.message.corefeature.data.remote.ResponseToResultMapper
import com.dino.message.corefeature.data.remote.dto.BaseResponse
import com.dino.message.spashscreen.domain.model.VersionCheckDecision
import kotlinx.serialization.Serializable

@Serializable
data class CheckAppVersionResponse(
    override val message: String? = null,
    val action: String,
    val is_blocking: Boolean,
    val title: String? = null,
    val download_url: String? = null,
    val normalized_version: String? = null,
) : BaseResponse(), ResponseToResultMapper<VersionCheckDecision> {

    override fun mapResponseToResult(): VersionCheckDecision {
        val normalizedAction = action.trim().lowercase()
        return VersionCheckDecision(
            action = normalizedAction,
            isBlocking = is_blocking,
            title = title?.trim().orEmpty(),
            message = message?.trim().orEmpty(),
            downloadUrl = download_url?.trim().orEmpty(),
            normalizedVersion = normalized_version?.trim().orEmpty(),
        )
    }
}
