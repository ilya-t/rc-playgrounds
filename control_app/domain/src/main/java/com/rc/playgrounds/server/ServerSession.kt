package com.rc.playgrounds.server

class ServerSession(
    private val screenWake: ScreenWake,
    private val keepAlive: ServerKeepAlive,
    private val pictureInPicture: ServerPictureInPicture,
    private val camera: ServerCamera,
) {
    fun start() {
        screenWake.acquire()
        keepAlive.start()
        camera.start()
    }

    fun onUserLeave() {
        pictureInPicture.enter()
    }

    fun onClosed() {
        camera.stop()
        screenWake.release()
        keepAlive.stop()
    }
}
