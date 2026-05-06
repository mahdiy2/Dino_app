package com.dino.message.chatfeature.presentation.screen

import android.app.Activity
import android.os.Handler
import android.os.Looper
import android.view.WindowManager
import androidx.core.view.ViewCompat

object WindowKeyboardController {

    private var originalMode: Int? = null

    fun enableResize(activity: Activity) {
        if (originalMode == null) {
            originalMode = activity.window.attributes.softInputMode
        }

        // Apply immediately
        activity.window.setSoftInputMode(
            WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
        )

        // Apply again after view attach
        activity.window.decorView.post {
            activity.window.setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
            )
            ViewCompat.requestApplyInsets(activity.window.decorView)
        }
    }

    fun restore(activity: Activity) {
        val mode = originalMode ?: return

        activity.window.setSoftInputMode(mode)

        activity.window.decorView.postOnAnimation {
            activity.window.setSoftInputMode(mode)
            ViewCompat.requestApplyInsets(activity.window.decorView)
        }

        Handler(Looper.getMainLooper()).postDelayed({
            activity.window.setSoftInputMode(mode)
            ViewCompat.requestApplyInsets(activity.window.decorView)
        }, 150)

        originalMode = null
    }
}
