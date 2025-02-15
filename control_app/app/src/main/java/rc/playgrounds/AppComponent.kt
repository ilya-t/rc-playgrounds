package rc.playgrounds

import com.testspace.App
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import rc.playgrounds.config.ConfigModel
import rc.playgrounds.control.SteeringController
import rc.playgrounds.control.gamepad.GamepadEventStream
import rc.playgrounds.storage.PersistentStorage

class AppComponent(app: App) {
    private val scope = CoroutineScope(Dispatchers.IO + CoroutineName("AppScope"))
    private val storage = PersistentStorage(app)
    val configModel = ConfigModel(
        scope,
        storage,
    )

    val gamepadEventStream = GamepadEventStream()
    private val steeringController = SteeringController(
        gamepadEventStream,
        scope,
        configModel,
    )

    companion object {
        lateinit var instance: AppComponent
    }
}