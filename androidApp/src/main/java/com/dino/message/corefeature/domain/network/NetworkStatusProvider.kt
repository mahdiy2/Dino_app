package com.dino.message.corefeature.domain.network

interface NetworkStatusProvider {
    fun isConnected(): Boolean
}
