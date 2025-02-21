package rc.playgrounds.stream

import android.net.Uri

interface StreamReceiver {
    fun play()
    fun release()
}