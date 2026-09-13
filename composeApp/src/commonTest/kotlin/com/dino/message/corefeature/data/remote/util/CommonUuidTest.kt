package com.dino.message.corefeature.data.remote.util

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CommonUuidTest {
    @Test
    fun randomUuidStringHasVersion4Shape() {
        val uuid = CommonUuid.randomUuidString()

        assertTrue(Regex("[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}").matches(uuid))
        assertEquals(36, uuid.length)
    }
}
