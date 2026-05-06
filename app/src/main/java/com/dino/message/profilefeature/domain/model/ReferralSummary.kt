package com.dino.message.profilefeature.domain.model

data class ReferralSummary(
    val referralCode: String,
    val joinedCount: Int,
    val requiredInvitesForVerified: Int,
    val remainingToVerified: Int,
    val isVerified: Boolean,
    val downloadLink: String,
)
