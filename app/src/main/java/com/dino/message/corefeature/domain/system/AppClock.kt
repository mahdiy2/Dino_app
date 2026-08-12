package com.dino.message.corefeature.domain.system

interface AppClock {
    fun currentTimeMillis(): Long
    fun uptimeMillis(): Long
}
