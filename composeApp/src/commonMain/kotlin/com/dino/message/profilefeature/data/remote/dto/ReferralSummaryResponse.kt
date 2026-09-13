package com.dino.message.profilefeature.data.remote.dto

import com.dino.message.corefeature.data.remote.ResponseToResultMapper
import com.dino.message.profilefeature.domain.model.ReferralSummary
import kotlinx.serialization.Serializable

@Serializable
data class ReferralSummaryResponse(
    val referral_code: String,
    val joined_count: Int = 0,
    val required_invites_for_verified: Int = 5,
    val remaining_to_verified: Int = 0,
    val is_verified: Boolean = false,
    val download_link: String = "https://api.dinosocial.ir",
) : ResponseToResultMapper<ReferralSummary> {
    override fun mapResponseToResult(): ReferralSummary {
        return ReferralSummary(
            referralCode = referral_code,
            joinedCount = joined_count,
            requiredInvitesForVerified = required_invites_for_verified,
            remainingToVerified = remaining_to_verified,
            isVerified = is_verified,
            downloadLink = download_link,
        )
    }
}
