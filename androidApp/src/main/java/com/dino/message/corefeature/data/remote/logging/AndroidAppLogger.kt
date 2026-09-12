package com.dino.message.corefeature.data.remote.logging

import android.util.Log
import com.dino.message.corefeature.domain.logging.AppLogger

object AndroidAppLogger : AppLogger {
    override fun debug(tag: String, message: String) {
        Log.d(tag, message)
    }

    override fun warning(tag: String, message: String, throwable: Throwable?) {
        if (throwable == null) {
            Log.w(tag, message)
        } else {
            Log.w(tag, message, throwable)
        }
    }

    override fun error(tag: String, message: String, throwable: Throwable?) {
        if (throwable == null) {
            Log.e(tag, message)
        } else {
            Log.e(tag, message, throwable)
        }
    }
}
