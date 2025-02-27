package rc.playgrounds.stream

import a.debug.stuff.ControlPanel
import android.annotation.SuppressLint
import android.net.Uri
import android.view.SurfaceHolder
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import com.testspace.core.Static
import org.freedesktop.gstreamer.GStreamerSurfaceView
import rc.playgrounds.config.ConfigModel

class StreamingProcess(
    private val configModel: ConfigModel,
    private val activity: AppCompatActivity,
    private val surfaceContainer: ViewGroup,
) {
    init {
        ControlPanel.setup(activity)
            .trigger("release") {
                streamReceiver.release()
                ControlPanel.setup(activity).print("RELEASED!")
            }
            .trigger("create") {
                streamReceiver = createReceiver()
                ControlPanel.setup(activity).print("CREATED!")
            }
            .trigger("play") {
                streamReceiver.play()
                ControlPanel.setup(activity).print("PLAYING!")
            }
    }
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

        return GStreamerReceiver(
            activity,
            surfaceContainer,
            configModel.configFlow.value.streamLocalCmd,
        )
    }

    fun start() {
        streamReceiver.play()
    }

    fun release() {
        streamReceiver.release()
    }
}