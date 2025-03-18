package rc.playgrounds.stream

import com.rc.playgrounds.stream.StreamReceiver

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