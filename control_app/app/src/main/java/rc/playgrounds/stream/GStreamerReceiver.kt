package rc.playgrounds.stream

import android.net.Uri
import android.view.SurfaceView
import androidx.appcompat.app.AppCompatActivity
import com.rc.playgrounds.gstreamer.GStreamerFacade
import com.rc.playgrounds.gstreamer.Logger
import com.testspace.core.Static

class GStreamerReceiver(
    activity: AppCompatActivity,
    surfaceView: SurfaceView,
    pipeline: String,
) : StreamReceiver {
    private val logger = object : Logger {
        override fun logError(e: Exception) {
            Static.output("GStreamer error: "+e.message)
        }

        override fun logMessage(message: String) {
            Static.output("GStreamer: " + message)
        }
    }

    private val gStreamerFacade = GStreamerFacade(
        activity,
        surfaceView,
        logger,
        pipeline,
    )

    override fun play() {
        gStreamerFacade.play()
    }

    override fun release() {
        // Runtime release not yet supported :(
//        gStreamerFacade.close()
    }
}
