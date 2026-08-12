package com.dino.message.corefeature.data.remote

import java.net.SocketTimeoutException
import java.nio.channels.UnresolvedAddressException

object JvmNetworkExceptionClassifier : NetworkExceptionClassifier {
    override fun isTimeout(throwable: Throwable, message: String): Boolean {
        return throwable is SocketTimeoutException ||
            CommonNetworkExceptionClassifier.isTimeout(throwable, message)
    }

    override fun isNetworkFailure(throwable: Throwable): Boolean {
        return throwable is UnresolvedAddressException ||
            CommonNetworkExceptionClassifier.isNetworkFailure(throwable)
    }
}
