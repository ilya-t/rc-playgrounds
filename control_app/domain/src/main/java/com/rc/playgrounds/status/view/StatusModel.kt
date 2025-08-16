package com.rc.playgrounds.status.view

import com.rc.playgrounds.config.ActiveConfigProvider
import com.rc.playgrounds.control.RcEvent
import com.rc.playgrounds.control.RcEventStream
import com.rc.playgrounds.remote.stream.RemoteStreamConfig
import com.rc.playgrounds.remote.stream.RemoteStreamConfigController
import com.rc.playgrounds.status.PingService
import com.rc.playgrounds.status.gstreamer.Event
import com.rc.playgrounds.status.gstreamer.FrameDropStatus
import com.rc.playgrounds.status.gstreamer.StreamerEvents
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlin.time.Duration

class StatusModel(
    private val scope: CoroutineScope,
    config: ActiveConfigProvider,
    private val rcEventStream: RcEventStream,
    private val streamerEvents: StreamerEvents,
    private val frameDropStatus: FrameDropStatus,
    private val remoteStreamConfigController: RemoteStreamConfigController,
) {

    private val pingTarget: Flow<String?> = config.configFlow.map { it.controlServer?.address }
    private val _text = MutableStateFlow("(?)")
    private val _ping = MutableStateFlow("(?)")
    val text: StateFlow<String> = _text

    init {
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
            combine(
                remoteStreamConfigController.state,
                rcEventStream.events,
                _ping,
                streamerEvents.events,
                frameDropStatus.frameDropsPerSecond,
                ::asStatus
            ).collect {
                _text.value = it
            }
        }
    }
}

private fun asStatus(
    streamConfig: RemoteStreamConfig?,
    event: RcEvent,
    ping: String,
    streamerEvent: Event,
    framesDropped: Int,
): String {
    val streamer: String? = if ((System.currentTimeMillis() - streamerEvent.time) < 10_000L) {
        when (streamerEvent) {
            is Event.Message -> null
            is Event.Error -> "streamer error: ${streamerEvent.error.message ?: streamerEvent.error.toString() }}"
        }
    } else {
        null
    }

    return buildString {
        if (streamConfig != null) {
            val p = streamConfig.parameters
            appendLine("- stream: ${p.width}x${p.height}\n  ${p.framerate}fps (${p.bitrate / 1_000_000f}mbit/s)")
        } else {
            appendLine("- stream: ?")
        }
        appendLine("- $ping")
        appendLine("- frameDrop/sec: $framesDropped")
        if (streamer != null) {
            appendLine("- $streamer")
        }

        appendLine("- long: %.2f (raw: %.2f) ".format(event.long, event.rawLong))
        appendLine("- steer: %.3f (raw: %.2f) ".format(event.steer, event.rawSteer))
        appendLine("- pitch: %.2f (raw: %.2f) ".format(event.pitch, event.rawPitch))
        appendLine("- yaw: %.2f (raw: %.2f)".format(event.yaw, event.rawYaw))
    }
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