package rc.playgrounds

import com.rc.playgrounds.status.view.StatusModel
import com.rc.playgrounds.stopwatch.StopwatchModel
import com.testspace.App
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import rc.playgrounds.config.ConfigModel
import com.rc.playgrounds.remote.OutputEventStream
import com.rc.playgrounds.control.gamepad.GamepadEventStream
import com.rc.playgrounds.remote.StreamCmdHash
import rc.playgrounds.storage.PersistentStorage

class AppComponent(app: App) {
    private val scope = CoroutineScope(Dispatchers.IO + CoroutineName("AppScope"))
    private val storage = PersistentStorage(app)
    val configModel = ConfigModel(
        scope,
        storage,
    )

    val gamepadEventStream = GamepadEventStream()
    val streamCmdHash = StreamCmdHash()
    private val outputEventStream = OutputEventStream(
        gamepadEventStream,
        scope,
        configModel,
        streamCmdHash,
    )

    val stopwatchModel = StopwatchModel(
        scope,
    )

    val statusModel = StatusModel(
        scope,
        configModel,
    )

    companion object {
        lateinit var instance: AppComponent
    }
}