package com.dino.message.corefeature.data.system

import android.os.Process
import com.dino.message.corefeature.domain.system.AppProcessController
import kotlin.system.exitProcess

class AndroidProcessController : AppProcessController {
    override fun terminateCurrentProcess(exitCode: Int) {
        Process.killProcess(Process.myPid())
        exitProcess(exitCode)
    }
}
