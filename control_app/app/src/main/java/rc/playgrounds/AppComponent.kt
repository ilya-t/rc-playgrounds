package rc.playgrounds

import com.rc.playgrounds.stopwatch.StopwatchModel
import com.testspace.App
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import rc.playgrounds.config.ConfigModel
import rc.playgrounds.control.SteeringController
import rc.playgrounds.control.gamepad.GamepadEventStream
import rc.playgrounds.stream.StreamCmdHash
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
    private val steeringController = SteeringController(
        gamepadEventStream,
        scope,
        configModel,
        streamCmdHash,
    )

    val stopwatchModel = StopwatchModel(
        scope,
    )

    companion object {
        lateinit var instance: AppComponent
    }
}