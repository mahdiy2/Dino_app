package com.dino.message.corefeature.data.remote

import com.dino.message.R
import com.dino.message.corefeature.data.remote.util.NetworkErrorMessageProvider
import com.dino.message.corefeature.domain.model.StringResourceContent
import com.dino.message.corefeature.domain.model.asStringResourceContent
import com.dino.message.corefeature.domain.resource.StringResolver

class AndroidNetworkErrorMessageProvider(
    private val stringResolver: StringResolver,
) : NetworkErrorMessageProvider {
    override fun badRequest(): StringResourceContent = R.string.error_bad_request.asStringResourceContent()
    override fun unauthorized(): StringResourceContent = R.string.error_unauthorized.asStringResourceContent()
    override fun forbidden(): StringResourceContent = R.string.error_forbidden.asStringResourceContent()
    override fun notFound(): StringResourceContent = R.string.error_not_found.asStringResourceContent()
    override fun conflict(): StringResourceContent = R.string.error_conflict.asStringResourceContent()
    override fun tooManyRequests(): StringResourceContent = R.string.error_too_many_login_attempts.asStringResourceContent()
    override fun httpStatus(statusCode: Int): StringResourceContent =
        stringResolver.resolveStringId(R.string.error_with_http_status_code, statusCode).asStringResourceContent()
    override fun payloadTooLarge(): StringResourceContent = R.string.error_payload_too_large.asStringResourceContent()
    override fun internalServerError(): StringResourceContent = R.string.internal_server_error.asStringResourceContent()
    override fun convertJsonResponse(): StringResourceContent = R.string.error_convert_json_response.asStringResourceContent()
    override fun unknownError(): StringResourceContent = R.string.unknown_error_occurred.asStringResourceContent()
    override fun requestTimedOut(): StringResourceContent = R.string.error_request_timed_out.asStringResourceContent()
    override fun networkError(): StringResourceContent = R.string.network_error.asStringResourceContent()
    override fun retriesExceeded(): StringResourceContent = R.string.error_retries_exceeded.asStringResourceContent()
}
