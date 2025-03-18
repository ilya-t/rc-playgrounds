package com.rc.playgrounds.stream

class StreamingProcess(
    private val receiverFactory: () -> StreamReceiver,
) {
    private var streamReceiver = receiverFactory()

    fun start() {
        streamReceiver.play()
    }

    fun release() {
        streamReceiver.release()
    }
}