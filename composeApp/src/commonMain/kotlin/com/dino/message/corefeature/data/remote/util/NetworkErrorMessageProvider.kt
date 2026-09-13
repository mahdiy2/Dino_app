package com.dino.message.corefeature.data.remote.util

import com.dino.message.corefeature.domain.model.StringResourceContent
import com.dino.message.corefeature.domain.model.AppStringKey
import com.dino.message.corefeature.domain.model.asStringResourceContent

interface NetworkErrorMessageProvider {
    fun badRequest(): StringResourceContent
    fun unauthorized(): StringResourceContent
    fun forbidden(): StringResourceContent
    fun notFound(): StringResourceContent
    fun conflict(): StringResourceContent
    fun tooManyRequests(): StringResourceContent
    fun httpStatus(statusCode: Int): StringResourceContent
    fun payloadTooLarge(): StringResourceContent
    fun internalServerError(): StringResourceContent
    fun convertJsonResponse(): StringResourceContent
    fun unknownError(): StringResourceContent
    fun requestTimedOut(): StringResourceContent
    fun networkError(): StringResourceContent
    fun retriesExceeded(): StringResourceContent
}

object DefaultNetworkErrorMessageProvider : NetworkErrorMessageProvider {
    override fun badRequest() = AppStringKey.ErrorBadRequest.asStringResourceContent()
    override fun unauthorized() = AppStringKey.ErrorUnauthorized.asStringResourceContent()
    override fun forbidden() = AppStringKey.ErrorForbidden.asStringResourceContent()
    override fun notFound() = AppStringKey.ErrorNotFound.asStringResourceContent()
    override fun conflict() = AppStringKey.ErrorConflict.asStringResourceContent()
    override fun tooManyRequests() = AppStringKey.ErrorTooManyLoginAttempts.asStringResourceContent()
    override fun httpStatus(statusCode: Int) = AppStringKey.ErrorWithHttpStatusCode.asStringResourceContent(statusCode)
    override fun payloadTooLarge() = AppStringKey.ErrorPayloadTooLarge.asStringResourceContent()
    override fun internalServerError() = AppStringKey.InternalServerError.asStringResourceContent()
    override fun convertJsonResponse() = AppStringKey.ErrorConvertJsonResponse.asStringResourceContent()
    override fun unknownError() = AppStringKey.UnknownErrorOccurred.asStringResourceContent()
    override fun requestTimedOut() = AppStringKey.ErrorRequestTimedOut.asStringResourceContent()
    override fun networkError() = AppStringKey.NetworkError.asStringResourceContent()
    override fun retriesExceeded() = AppStringKey.ErrorRetriesExceeded.asStringResourceContent()
}
