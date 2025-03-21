package com.rc.playgrounds

import android.app.Application
import com.rc.playgrounds.config.ConfigModel
import com.rc.playgrounds.config.ConfigRepository
import com.rc.playgrounds.control.SteeringEventStream
import com.rc.playgrounds.control.gamepad.GamepadEventStream
import com.rc.playgrounds.remote.OutputEventStream
import com.rc.playgrounds.remote.StreamCmdHash
import com.rc.playgrounds.status.gstreamer.FrameDropStatus
import com.rc.playgrounds.status.gstreamer.StreamerEvents
import com.rc.playgrounds.status.view.StatusModel
import com.rc.playgrounds.stopwatch.StopwatchModel
import com.rc.playgrounds.storage.AndroidPersistentStorage
import com.rc.playgrounds.storage.PersistentStorage
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

class AppComponent(app: Application) {
    private val scope = CoroutineScope(Dispatchers.IO + CoroutineName("AppScope"))
    private val storage: PersistentStorage = AndroidPersistentStorage(app)
    private val configRepository = ConfigRepository(
        storage,
        scope,
    )
    val configModel = ConfigModel(
        scope,
        configRepository,
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

    val streamerEvents = StreamerEvents(scope)
    private val frameDropStatus = FrameDropStatus(
        streamerEvents.events,
        scope,
    )

    val statusModel = StatusModel(
        scope,
        configModel,
        steeringEventStream,
        streamerEvents,
        frameDropStatus,
    )

    companion object {
        lateinit var instance: AppComponent
    }
}