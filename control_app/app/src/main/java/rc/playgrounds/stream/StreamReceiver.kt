package rc.playgrounds.stream

import android.net.Uri

interface StreamReceiver {
    fun play(uri: Uri)
    fun release()
}