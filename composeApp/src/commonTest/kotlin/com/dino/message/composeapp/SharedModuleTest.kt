package com.dino.message.composeapp

import kotlin.test.Test
import kotlin.test.assertSame

class SharedModuleTest {
    @Test
    fun sharedModuleIsAvailable() {
        assertSame(SharedModule, SharedModule)
    }
}
