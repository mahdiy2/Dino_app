package com.dino.message.corefeature.data.remote

import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.utils.io.errors.IOException

interface NetworkExceptionClassifier {
    fun isTimeout(throwable: Throwable, message: String = throwable.message.orEmpty()): Boolean
    fun isNetworkFailure(throwable: Throwable): Boolean
}

object CommonNetworkExceptionClassifier : NetworkExceptionClassifier {
    override fun isTimeout(throwable: Throwable, message: String): Boolean {
        return throwable is HttpRequestTimeoutException || isTimeoutLikeMessage(message)
    }

    override fun isNetworkFailure(throwable: Throwable): Boolean {
        return throwable is IOException
    }

    fun isTimeoutLikeMessage(message: String?): Boolean {
        val normalized = message?.trim()?.lowercase().orEmpty()
        if (normalized.isBlank()) return false
        return normalized.contains("timeout") || normalized.contains("timed out")
    }
}
