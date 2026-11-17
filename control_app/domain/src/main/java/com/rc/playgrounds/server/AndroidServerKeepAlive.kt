package com.rc.playgrounds.server

import android.content.Context
import android.content.Intent

class AndroidServerKeepAlive(
    private val context: Context,
) : ServerKeepAlive {
    override fun start() {
        context.startForegroundService(Intent(context, ServerForegroundService::class.java))
    }

    override fun stop() {
        context.stopService(Intent(context, ServerForegroundService::class.java))
    }
}
