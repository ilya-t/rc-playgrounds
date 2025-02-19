package rc.playgrounds.stream

import android.annotation.SuppressLint
import android.net.Uri
import android.view.SurfaceHolder
import androidx.appcompat.app.AppCompatActivity
import org.freedesktop.gstreamer.GStreamerSurfaceView

class StreamingProcess(
    private val activity: AppCompatActivity,
//    private val textureView: TextureView,
//    private val playerView: PlayerView,
//    private val surfaceView: SurfaceView,
    private val gSurfaceView: GStreamerSurfaceView,
) {
    // GStreamerReceiver currently cannot survive release.
    private val gStreamerReceiverSingleton = GStreamerReceiver(
        activity,
        gSurfaceView,
    )
    private var streamReceiver = createReceiver()

    @SuppressLint("UnsafeOptInUsageError")
    private fun createReceiver(): StreamReceiver {
//        return ExoReceiver(
//            activity,
//            playerView,
//        )
//        return VLCStreamReceiver(
//            activity,
//            surfaceView
//        )
        return gStreamerReceiverSingleton
    }

    fun start(streamUri:Uri) {
        gSurfaceView.holder.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceCreated(holder: SurfaceHolder) {
                streamReceiver.play(streamUri)
            }

            override fun surfaceDestroyed(holder: SurfaceHolder) {
                streamReceiver.release()
            }

            override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
                streamReceiver.release()
                streamReceiver = createReceiver()
                streamReceiver.play(streamUri)
            }
        })

        if (!gSurfaceView.holder.isCreating) {
            streamReceiver.play(streamUri)
        }

    }

    fun release() {
        streamReceiver.release()
    }
}