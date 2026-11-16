package com.rc.playgrounds.server

import android.app.Activity
import android.view.WindowManager

class AndroidScreenWake(
    private val activity: Activity,
) : ScreenWake {
    override fun acquire() {
        activity.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    override fun release() {
        activity.window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }
}
