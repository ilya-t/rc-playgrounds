package com.rc.playgrounds.server

class ServerSession(
    private val screenWake: ScreenWake,
    private val pictureInPicture: ServerPictureInPicture,
) {
    fun start() {
        screenWake.acquire()
    }

    fun onUserLeave() {
        pictureInPicture.enter()
    }

    fun onClosed() {
        screenWake.release()
    }
}
