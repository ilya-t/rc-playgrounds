package com.rc.playgrounds.server

interface ServerVideoSender : ServerCameraFrames {
    fun start()

    fun stop()
}
