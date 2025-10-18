package com.rc.playgrounds

import android.app.Application
import com.rc.playgrounds.config.ActiveConfigProvider
import com.rc.playgrounds.config.ConfigRepository
import com.rc.playgrounds.config.view.ConfigModel
import com.rc.playgrounds.control.ControlInterpolationProvider
import com.rc.playgrounds.control.ControlTuningProvider
import com.rc.playgrounds.control.RcEventStream
import com.rc.playgrounds.control.gamepad.GamePadEventSessionProvider
import com.rc.playgrounds.control.gamepad.GamepadEventStream
import com.rc.playgrounds.control.lock.ControlLock
import com.rc.playgrounds.control.quick.QuickConfigState
import com.rc.playgrounds.control.steering.SteerProvider
import com.rc.playgrounds.fullscreen.FullscreenStateController
import com.rc.playgrounds.navigation.ActiveScreenProvider
import com.rc.playgrounds.presentation.announce.AnnounceModel
import com.rc.playgrounds.presentation.lock.LockModel
import com.rc.playgrounds.presentation.main.MainModel
import com.rc.playgrounds.presentation.overlay.OverlayModel
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
    private val gamePadEventSessionProvider = GamePadEventSessionProvider(
        scope,
        gamepadEventStream,
    )
    private val controlLock = ControlLock()
    private val controlTuningProvider = ControlTuningProvider(
        activeConfigProvider,
        scope,
    )
    private val controlInterpolationProvider = ControlInterpolationProvider(
        controlTuningProvider,
        activeConfigProvider,
    )
    private val steerProvider = SteerProvider(
        gamePadEventSessionProvider,
        controlInterpolationProvider,
        activeConfigProvider,
        controlTuningProvider,
        scope,
    )
    private val rcEventStream = RcEventStream(
        scope,
        controlInterpolationProvider,
        activeConfigProvider,
        gamepadEventStream,
        steerProvider,
        controlLock,
    )
    val streamCmdHash = StreamCmdHash()

    private val streamQualityProvider = StreamQualityProvider(
        activeConfigProvider,
        scope,
    )
    private val quickConfigState = QuickConfigState()

    val mainModel = MainModel(
        activeScreenProvider,
        scope,
        gamepadEventStream,
        controlLock,
        quickConfigState,
    )

    val quickConfigModel = QuickConfigModel(
        scope,
        activeScreenProvider,
        activeConfigProvider,
        streamQualityProvider,
        quickConfigState,
    )
    private val remoteStreamConfigController = RemoteStreamConfigController(
        activeConfigProvider,
        streamQualityProvider,
        scope,
    )
    private val outputEventStream = OutputEventStream(
        rcEventStream,
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
        rcEventStream,
        streamerEvents,
        frameDropStatus,
        remoteStreamConfigController,
        controlTuningProvider,
        quickConfigModel,
    )

    val lockModel = LockModel(
        controlLock,
    )

    val announceModel = AnnounceModel(
        activeConfigProvider,
        quickConfigModel,
        gamepadEventStream,
        scope,
    )

    val overlayModel = OverlayModel(
        activeScreenProvider,
        lockModel,
        announceModel,
        quickConfigModel,
    )

    companion object {
        lateinit var instance: AppComponent
    }
}