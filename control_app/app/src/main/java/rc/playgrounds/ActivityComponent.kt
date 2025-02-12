package rc.playgrounds

import android.net.Uri
import android.view.SurfaceView
import android.view.TextureView
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.media3.ui.PlayerView
import com.testspace.R
import rc.playgrounds.stream.StreamingProcess

class ActivityComponent(
    private val a: AppCompatActivity,
) {
    private val playerView = a.findViewById<PlayerView>(R.id.player_view)
    private val surfaceView = a.findViewById<SurfaceView>(R.id.surface_view)
    private val textureView = a.findViewById<TextureView>(R.id.texture_view)
    private val resetButton = a.findViewById<View>(R.id.reset_button)
    private var streamingProcess: StreamingProcess? = null
    val streamUri = Uri.parse(
        "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8"
//        "udp://@:12345"
    )
    init {
        start()

        resetButton.setOnClickListener {
            release()
            start()
        }

        a.lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onDestroy(owner: LifecycleOwner) {
                release()
            }

        })
    }

    private fun start() {
        streamingProcess?.release()
        streamingProcess = StreamingProcess(a, surfaceView)
        streamingProcess?.start(streamUri)
    }

    private fun release() {
        streamingProcess?.release()
        streamingProcess = null

    }
}