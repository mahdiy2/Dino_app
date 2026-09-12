package com.dino.message.corefeature.domain.logging

interface AppLogger {
    fun debug(tag: String, message: String)
    fun warning(tag: String, message: String, throwable: Throwable? = null)
    fun error(tag: String, message: String, throwable: Throwable? = null)
}
