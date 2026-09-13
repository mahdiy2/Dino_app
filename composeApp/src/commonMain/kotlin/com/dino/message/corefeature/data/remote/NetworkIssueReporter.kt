package com.dino.message.corefeature.data.remote

interface NetworkIssueReporter {
    fun reportNonFatalIssue(
        issueType: String,
        message: String,
        details: String? = null,
        category: String = "app",
    )
}

object NoOpNetworkIssueReporter : NetworkIssueReporter {
    override fun reportNonFatalIssue(
        issueType: String,
        message: String,
        details: String?,
        category: String,
    ) = Unit
}
