package rc.playgrounds.stream

import android.net.Uri
import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.appcompat.app.AppCompatActivity
import rc.playgrounds.StreamReceiver

class StreamingProcess(
    private val activity: AppCompatActivity,
    private val surfaceView: SurfaceView,
) {
    private var streamReceiver = StreamReceiver(
        activity,
        surfaceView
    )

    fun start(streamUri:Uri) {

        surfaceView.holder.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceCreated(holder: SurfaceHolder) {
                streamReceiver.play(streamUri)
            }

            override fun surfaceDestroyed(holder: SurfaceHolder) {
                streamReceiver.release()
            }

            override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
                streamReceiver.release()
                streamReceiver = StreamReceiver(activity, surfaceView)
                streamReceiver.play(streamUri)
            }
        })

        if (!surfaceView.holder.isCreating) {
            streamReceiver.play(streamUri)
        }

    }

    fun release() {
        streamReceiver.release()
    }
}