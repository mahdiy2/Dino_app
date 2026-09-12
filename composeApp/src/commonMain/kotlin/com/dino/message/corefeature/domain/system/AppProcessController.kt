package com.dino.message.corefeature.domain.system

interface AppProcessController {
    fun terminateCurrentProcess(exitCode: Int)
}
