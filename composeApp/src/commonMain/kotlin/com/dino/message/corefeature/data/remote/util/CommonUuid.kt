package com.dino.message.corefeature.data.remote.util

import kotlin.random.Random

object CommonUuid {
    fun randomUuidString(): String {
        val bytes = Random.Default.nextBytes(16)
        bytes[6] = ((bytes[6].toInt() and 0x0f) or 0x40).toByte()
        bytes[8] = ((bytes[8].toInt() and 0x3f) or 0x80).toByte()
        return buildString(36) {
            bytes.forEachIndexed { index, byte ->
                if (index == 4 || index == 6 || index == 8 || index == 10) append('-')
                append((byte.toInt() and 0xff).toString(16).padStart(2, '0'))
            }
        }
    }
}
