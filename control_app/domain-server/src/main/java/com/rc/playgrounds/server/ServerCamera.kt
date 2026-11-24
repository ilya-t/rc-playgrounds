package com.rc.playgrounds.server

import kotlinx.coroutines.flow.Flow

fun interface ServerCameraFrames {
    fun onFrame(frame: ByteArray, width: Int, height: Int)
}

interface ServerCamera {
    val state: Flow<ServerCameraState>

    fun start()

    fun stop()

    fun setFrameListener(listener: ServerCameraFrames?)
}

enum class ServerCameraState {
    Stopped,
    Starting,
    Previewing,
    Missing,
    CameraDenied,
    Denied,
    Failed,
}
