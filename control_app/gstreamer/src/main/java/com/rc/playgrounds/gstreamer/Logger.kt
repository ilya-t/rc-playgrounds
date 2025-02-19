package com.rc.playgrounds.gstreamer

interface Logger {
    fun logError(e: Exception)
    fun logMessage(message: String)
}