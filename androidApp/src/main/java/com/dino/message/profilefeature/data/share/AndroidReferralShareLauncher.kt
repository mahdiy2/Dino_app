package com.dino.message.profilefeature.data.share

import android.content.Context
import android.content.Intent
import com.dino.message.R
import com.dino.message.profilefeature.domain.share.ReferralShareLauncher

class AndroidReferralShareLauncher(
    private val context: Context,
) : ReferralShareLauncher {
    override fun shareInvite(text: String) {
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        val chooser = Intent.createChooser(
            sendIntent,
            context.getString(R.string.referral_share_sheet_title)
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }
}
