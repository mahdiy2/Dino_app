package com.dino.message.corefeature.domain.session

interface SessionProvider {
    fun hasActiveSession(): Boolean
}
