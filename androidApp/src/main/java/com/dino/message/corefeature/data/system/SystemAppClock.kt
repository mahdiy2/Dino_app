package com.dino.message.corefeature.data.system

import android.os.SystemClock
import com.dino.message.corefeature.domain.system.AppClock

class SystemAppClock : AppClock {
    override fun currentTimeMillis(): Long = System.currentTimeMillis()

    override fun uptimeMillis(): Long = SystemClock.uptimeMillis()
}
