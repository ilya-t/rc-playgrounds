package com.rc.playgrounds.server

class ServerSession(
    private val screenWake: ScreenWake,
    private val keepAlive: ServerKeepAlive,
    private val pictureInPicture: ServerPictureInPicture,
    private val camera: ServerCamera,
    private val videoSender: ServerVideoSender,
) {
    fun start() {
        screenWake.acquire()
        keepAlive.start()
        videoSender.start()
        camera.setFrameListener(videoSender)
        camera.start()
    }

    fun onUserLeave() {
        pictureInPicture.enter()
    }

    fun onClosed() {
        camera.setFrameListener(null)
        camera.stop()
        videoSender.stop()
        screenWake.release()
        keepAlive.stop()
    }
}
