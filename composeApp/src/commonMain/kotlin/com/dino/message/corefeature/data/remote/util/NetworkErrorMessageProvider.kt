package com.dino.message.corefeature.data.remote.util

import com.dino.message.corefeature.domain.model.StringResourceContent
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
    override fun badRequest() = "There is a problem with the request".asStringResourceContent()
    override fun unauthorized() = "The required documents to verify the identity of the request were not found".asStringResourceContent()
    override fun forbidden() = "There are no permissions required to access the resource".asStringResourceContent()
    override fun notFound() = "Not found".asStringResourceContent()
    override fun conflict() = "Conflict".asStringResourceContent()
    override fun tooManyRequests() = "Too many login attempts, try again later.".asStringResourceContent()
    override fun httpStatus(statusCode: Int) = "Error with HTTP status code $statusCode".asStringResourceContent()
    override fun payloadTooLarge() = "File is too large to upload. Please choose a smaller file.".asStringResourceContent()
    override fun internalServerError() = "Internal server error".asStringResourceContent()
    override fun convertJsonResponse() = "Error in converting json response".asStringResourceContent()
    override fun unknownError() = "Unknown error occurred".asStringResourceContent()
    override fun requestTimedOut() = "Request timed out".asStringResourceContent()
    override fun networkError() = "Network error".asStringResourceContent()
    override fun retriesExceeded() = "Retry failed".asStringResourceContent()
}
