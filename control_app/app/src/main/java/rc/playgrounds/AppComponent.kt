package rc.playgrounds

import com.testspace.App
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import rc.playgrounds.config.ConfigModel
import rc.playgrounds.telemetry.gamepad.GamepadEventStream
import rc.playgrounds.storage.PersistentStorage
import rc.playgrounds.telemetry.TelemetryController

class AppComponent(app: App) {
    private val scope = CoroutineScope(Dispatchers.IO + CoroutineName("AppScope"))
    private val storage = PersistentStorage(app)
    val configModel = ConfigModel(
        scope,
        storage,
    )

    val gamepadEventStream = GamepadEventStream()
    private val telemetryEmitter = TelemetryController(
        gamepadEventStream,
        scope,
        configModel,
    )

    companion object {
        lateinit var instance: AppComponent
    }
}