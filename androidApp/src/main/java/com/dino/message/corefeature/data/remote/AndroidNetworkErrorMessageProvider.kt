package com.dino.message.corefeature.data.remote

import com.dino.message.corefeature.data.remote.util.NetworkErrorMessageProvider
import com.dino.message.corefeature.domain.model.AppStringKey
import com.dino.message.corefeature.domain.model.StringResourceContent
import com.dino.message.corefeature.domain.model.asStringResourceContent

class AndroidNetworkErrorMessageProvider : NetworkErrorMessageProvider {
    override fun badRequest(): StringResourceContent = AppStringKey.ErrorBadRequest.asStringResourceContent()
    override fun unauthorized(): StringResourceContent = AppStringKey.ErrorUnauthorized.asStringResourceContent()
    override fun forbidden(): StringResourceContent = AppStringKey.ErrorForbidden.asStringResourceContent()
    override fun notFound(): StringResourceContent = AppStringKey.ErrorNotFound.asStringResourceContent()
    override fun conflict(): StringResourceContent = AppStringKey.ErrorConflict.asStringResourceContent()
    override fun tooManyRequests(): StringResourceContent = AppStringKey.ErrorTooManyLoginAttempts.asStringResourceContent()
    override fun httpStatus(statusCode: Int): StringResourceContent =
        AppStringKey.ErrorWithHttpStatusCode.asStringResourceContent(statusCode)
    override fun payloadTooLarge(): StringResourceContent = AppStringKey.ErrorPayloadTooLarge.asStringResourceContent()
    override fun internalServerError(): StringResourceContent = AppStringKey.InternalServerError.asStringResourceContent()
    override fun convertJsonResponse(): StringResourceContent = AppStringKey.ErrorConvertJsonResponse.asStringResourceContent()
    override fun unknownError(): StringResourceContent = AppStringKey.UnknownErrorOccurred.asStringResourceContent()
    override fun requestTimedOut(): StringResourceContent = AppStringKey.ErrorRequestTimedOut.asStringResourceContent()
    override fun networkError(): StringResourceContent = AppStringKey.NetworkError.asStringResourceContent()
    override fun retriesExceeded(): StringResourceContent = AppStringKey.ErrorRetriesExceeded.asStringResourceContent()
}
