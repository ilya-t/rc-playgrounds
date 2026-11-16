package com.rc.playgrounds.server

class ServerSession(
    private val pictureInPicture: ServerPictureInPicture,
) {
    fun onUserLeave() {
        pictureInPicture.enter()
    }
}
