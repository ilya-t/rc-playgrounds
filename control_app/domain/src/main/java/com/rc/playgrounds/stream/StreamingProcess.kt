package com.rc.playgrounds.stream

import com.rc.playgrounds.config.Config

class StreamingProcess(
    private val config: Config,
    private val receiverFactory: (Config) -> StreamReceiver,
) {
    private var streamReceiver = receiverFactory(config)

    fun start() {
        streamReceiver.play()
    }

    fun release() {
        streamReceiver.release()
    }
}