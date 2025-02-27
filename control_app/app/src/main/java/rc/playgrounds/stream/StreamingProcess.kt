package rc.playgrounds.stream

import android.annotation.SuppressLint
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import rc.playgrounds.config.ConfigModel

class StreamingProcess(
    private val configModel: ConfigModel,
    private val activity: AppCompatActivity,
    private val surfaceContainer: ViewGroup,
) {
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