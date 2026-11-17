package com.rc.playgrounds.server

import kotlinx.coroutines.flow.Flow

interface ServerCamera {
    val state: Flow<ServerCameraState>

    fun start()

    fun stop()
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
