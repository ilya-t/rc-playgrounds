package com.rc.playgrounds.status.view

import com.rc.playgrounds.config.ActiveConfigProvider
import com.rc.playgrounds.control.RcEvent
import com.rc.playgrounds.control.RcEventStream
import com.rc.playgrounds.presentation.quickconfig.EnvironmentOverrides
import com.rc.playgrounds.presentation.quickconfig.OverrideProfile
import com.rc.playgrounds.presentation.quickconfig.QuickConfigModel
import com.rc.playgrounds.presentation.quickconfig.QuickConfigViewModel
import com.rc.playgrounds.status.PingService
import com.rc.playgrounds.status.gstreamer.Event
import com.rc.playgrounds.status.gstreamer.FrameDropStatus
import com.rc.playgrounds.status.gstreamer.StreamerEvents
import com.rc.playgrounds.telemetry.Telemetry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlin.time.Duration

class StatusModel(
    private val scope: CoroutineScope,
    config: ActiveConfigProvider,
    private val rcEventStream: RcEventStream,
    private val streamerEvents: StreamerEvents,
    private val frameDropStatus: FrameDropStatus,
    private val quickConfigModel: QuickConfigModel,
    telemetry: Flow<Telemetry?>,
) {
    private val lastStreamerError = MutableStateFlow<Event.Error?>(null)
    private val pingTarget: Flow<String?> = config.configFlow.map { it.controlServer?.address(it.env) }
    private val _text = MutableStateFlow("")
    private val _ping = MutableStateFlow("ping: not received yet")
    private val canShowStatus = MutableStateFlow(true)
    val text: StateFlow<String> = _text

    init {
        scope.launch {
            streamerEvents.events.filterIsInstance<Event.Error>().collect {
                lastStreamerError.value = it
            }
        }
        scope.launch {
            var collector: StatusCollector? = null
            pingTarget.collect {
                collector?.close()
                collector = StatusCollector(
                    server = it,
                    scope = scope,
                ) {
                    _ping.value = it
                }
            }
        }

        scope.launch {
            quickConfigModel.viewModel.collect { viewModel ->
                canShowStatus.value = when (viewModel) {
                    is QuickConfigViewModel.Hidden -> true
                    is QuickConfigViewModel.Visible -> false
                }
            }
        }

        scope.launch {
            val statusText: Flow<String> = combine(
                rcEventStream.events,
                _ping,
                lastStreamerError,
                //TODO: maybe bring back? frameDropStatus.frameDropsPerSecond,
                config.configFlow.map { it.envOverrides },
                telemetry,
                ::asStatus
            )

            combine(
                statusText,
                canShowStatus,
            ){ text, canShow ->
                if (canShow) text else ""
            }.collect {
                _text.value = it
            }
        }
    }
}

private fun asStatus(
    event: RcEvent,
    ping: String,
    streamerEvent: Event.Error?,
    envOverrides: List<EnvironmentOverrides>,
    telemetry: Telemetry?,
): String {
    return buildString {
        appendLine("- $ping")
        if (telemetry == null) {
            appendLine("- telemetry: $CONTROL_ERROR_EMOJI")
        } else {
            appendLine(controlLine(telemetry.control))
            telemetry.cellular?.let { appendLine(cellularLine(it)) }
            telemetry.battery?.let { appendLine(batteryLine(it)) }
        }

        appendLine("- long: %.2f (raw: %.2f) ".format(event.long, event.rawLong))
        appendLine("- steer: %.3f (raw: %.2f) ".format(event.steer, event.rawSteer))
        appendLine("- pitch: %.2f (raw: %.2f) ".format(event.pitch, event.rawPitch))
        appendLine("- yaw: %.2f (raw: %.2f)".format(event.yaw, event.rawYaw))

        if (streamerEvent != null && (System.currentTimeMillis() - streamerEvent.time) < 10_000L) {
            appendLine("- streamer error(!): ${streamerEvent.error.message ?: streamerEvent.error.toString() }}")
        }

        envOverrides.forEach { overrides ->
            val activeProfile: OverrideProfile = overrides.profiles.getOrNull(overrides.lastActiveIndex ?: -1)
                ?: return@forEach
            appendLine("- ${overrides.name}: ${activeProfile.name}")
        }
    }
}

private fun controlLine(control: Telemetry.Control): String {
    val status = control.status.take(CONTROL_STATUS_LIMIT)
    val message = if (control.error) "$CONTROL_ERROR_EMOJI $status" else status
    return "- control: $message"
}

internal fun cellularLine(cellular: Telemetry.Cellular): String {
    val strength = cellular.strength
    val data = if (strength == null) "${cellular.generation}" else "$strength% ${cellular.generation}"
    return "- signal: $data"
}

internal fun batteryLine(battery: Telemetry.Battery): String {
    val charging = if (battery.charging) " charging" else ""
    return "- server.battery: ${battery.charge}%$charging"
}

private class StatusCollector(
    server: String?,
    scope: CoroutineScope,
    statusReceiver: (String) -> Unit
) {
    private val pingService = PingService(scope, server.orEmpty())
    private val pingServiceJob = pingService.start()
    private val statusJob = scope.launch {
        pingService.pingResult.collect { ping ->
            ping
                .onSuccess { duration: Duration ->
                    statusReceiver("ping: ${duration.inWholeMilliseconds}ms")
                }
                .onFailure {
                    statusReceiver("ping: ${it.message ?: "(error)"} ${SAD_EMOJI.random()}")
                }
        }
    }

    fun close() {
        pingServiceJob.cancel()
        statusJob.cancel()
    }
}

private const val CONTROL_STATUS_LIMIT = 25
private const val CONTROL_ERROR_EMOJI = "❌"

private val SAD_EMOJI = listOf(
    "🥲",
    "🤨",
    "😟",
    "😕",
    "🙁",
    "☹️",
    "😣",
    "😫",
    "😩",
    "😢",
    "😭",
    "😮‍💨",
    "😤",
    "😠",
    "😥",
    "🫠",
    "😵",
    "😲",
    "😡",
    "🤯",
    "😔",
    "🤬",
    "😨",
    "😰",
    "😱",
    "🥶",
    "🫣",
    "😴",
    "🥱",
)