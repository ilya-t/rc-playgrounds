package com.rc.playgrounds

import android.app.Application
import com.rc.playgrounds.control.SteeringEventStream
import com.rc.playgrounds.control.gamepad.GamepadEventStream
import com.rc.playgrounds.remote.OutputEventStream
import com.rc.playgrounds.remote.StreamCmdHash
import com.rc.playgrounds.status.view.StatusModel
import com.rc.playgrounds.stopwatch.StopwatchModel
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import rc.playgrounds.config.ConfigModel
import rc.playgrounds.storage.PersistentStorage

class AppComponent(app: Application) {
    private val scope = CoroutineScope(Dispatchers.IO + CoroutineName("AppScope"))
    private val storage = PersistentStorage(app)
    val configModel = ConfigModel(
        scope,
        storage,
    )

    val gamepadEventStream = GamepadEventStream()
    private val steeringEventStream = SteeringEventStream(
        scope,
        configModel,
        gamepadEventStream,
    )
    val streamCmdHash = StreamCmdHash()
    private val outputEventStream = OutputEventStream(
        steeringEventStream,
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
        steeringEventStream,
    )

    companion object {
        lateinit var instance: AppComponent
    }
}