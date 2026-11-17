package com.rc.playgrounds.server

class ServerSession(
    private val screenWake: ScreenWake,
    private val keepAlive: ServerKeepAlive,
    private val pictureInPicture: ServerPictureInPicture,
) {
    fun start() {
        screenWake.acquire()
        keepAlive.start()
    }

    fun onUserLeave() {
        pictureInPicture.enter()
    }

    fun onClosed() {
        screenWake.release()
        keepAlive.stop()
    }
}
