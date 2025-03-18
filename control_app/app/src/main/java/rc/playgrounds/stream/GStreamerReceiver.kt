package rc.playgrounds.stream

import android.view.SurfaceView
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import com.rc.playgrounds.gstreamer.GStreamerFacade
import com.rc.playgrounds.gstreamer.Logger
import com.rc.playgrounds.stream.StreamReceiver
import com.testspace.core.Static

class GStreamerReceiver(
    activity: AppCompatActivity,
    private val surfaceContainer: ViewGroup,
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

    private val surfaceView = createSurfaceAt(surfaceContainer)

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
        gStreamerFacade.close()
        surfaceContainer.removeAllViews()
    }
}

private fun createSurfaceAt(container: ViewGroup): SurfaceView {
    container.removeAllViews()
    val surfaceView = SurfaceView(container.context)
    container.addView(surfaceView, ViewGroup.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
    return surfaceView
}