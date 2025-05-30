package com.rc.playgrounds

import android.app.Application
import com.rc.playgrounds.config.ActiveConfigProvider
import com.rc.playgrounds.config.ConfigRepository
import com.rc.playgrounds.config.view.ConfigModel
import com.rc.playgrounds.control.SteeringEventStream
import com.rc.playgrounds.control.gamepad.GamepadEventStream
import com.rc.playgrounds.control.lock.ControlLock
import com.rc.playgrounds.fullscreen.FullscreenStateController
import com.rc.playgrounds.navigation.ActiveScreenProvider
import com.rc.playgrounds.presentation.lock.LockModel
import com.rc.playgrounds.presentation.main.MainModel
import com.rc.playgrounds.presentation.quickconfig.QuickConfigModel
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
    val activeScreenProvider = ActiveScreenProvider()
    val configRepository = ConfigRepository(
        storage,
        scope,
    )

    val fullScreenStateController = FullscreenStateController(
        scope,
    )
    val activeConfigProvider = ActiveConfigProvider(
        configRepository,
        scope,
    )
    val configModel = ConfigModel(
        configRepository = configRepository,
        scope = scope,
    )

    val gamepadEventStream = GamepadEventStream(
        scope,
    )
    private val controlLock = ControlLock()
    private val steeringEventStream = SteeringEventStream(
        scope,
        activeConfigProvider,
        gamepadEventStream,
        controlLock,
    )
    val streamCmdHash = StreamCmdHash()

    private val streamQualityProvider = StreamQualityProvider(
        activeConfigProvider,
        scope,
    )

    val mainModel = MainModel(
        activeScreenProvider,
        scope,
        gamepadEventStream,
        controlLock,
    )

    val quickConfigModel = QuickConfigModel(
        scope,
        activeScreenProvider,
        activeConfigProvider,
        streamQualityProvider,
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

    val lockModel = LockModel(
        controlLock,
    )

    companion object {
        lateinit var instance: AppComponent
    }
}