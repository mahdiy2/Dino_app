package com.dino.message.reportfeature.data.remote

import com.dino.message.corefeature.data.remote.BaseRoute

sealed class ReportRoute(url: String) : BaseRoute(url) {
    object GetReportTypes : ReportRoute("api/report-types")

    data class CreatePostReport(
        val postId: Long,
    ) : ReportRoute("api/posts/$postId/reports")
}
