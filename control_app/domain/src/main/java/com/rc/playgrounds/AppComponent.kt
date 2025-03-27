package com.rc.playgrounds

import android.app.Application
import com.rc.playgrounds.config.ActiveConfigProvider
import com.rc.playgrounds.config.ConfigRepository
import com.rc.playgrounds.config.view.ConfigModel
import com.rc.playgrounds.control.SteeringEventStream
import com.rc.playgrounds.control.gamepad.GamepadEventStream
import com.rc.playgrounds.fullscreen.FullscreenStateController
import com.rc.playgrounds.remote.OutputEventStream
import com.rc.playgrounds.remote.StreamCmdHash
import com.rc.playgrounds.remote.stream.RemoteStreamConfigController
import com.rc.playgrounds.remote.stream.StreamQualityProvider
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
    val configRepository = ConfigRepository(
        storage,
        scope,
    )

    val fullScreenStateController = FullscreenStateController(
        scope,
    )
    val activeConfigProvider = ActiveConfigProvider(
        scope,
        configRepository,
    )
    val configModel = ConfigModel(
        configRepository = configRepository,
        scope = scope,
    )

    val gamepadEventStream = GamepadEventStream(
        scope,
    )
    private val steeringEventStream = SteeringEventStream(
        scope,
        activeConfigProvider,
        gamepadEventStream,
    )
    val streamCmdHash = StreamCmdHash()

    private val streamQualityProvider = StreamQualityProvider(
        activeConfigProvider,
        gamepadEventStream,
        scope,
    )
    private val remoteStreamConfigController = RemoteStreamConfigController(
        activeConfigProvider,
        streamQualityProvider,
        scope,
    )
    private val outputEventStream = OutputEventStream(
        steeringEventStream,
        scope,
        activeConfigProvider,
        streamCmdHash,
        remoteStreamConfigController,
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
        activeConfigProvider,
        steeringEventStream,
        streamerEvents,
        frameDropStatus,
        remoteStreamConfigController,
    )

    companion object {
        lateinit var instance: AppComponent
    }
}