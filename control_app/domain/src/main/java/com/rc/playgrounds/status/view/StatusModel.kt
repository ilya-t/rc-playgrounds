package com.rc.playgrounds.status.view

import com.rc.playgrounds.control.SteeringEvent
import com.rc.playgrounds.control.SteeringEventStream
import com.rc.playgrounds.status.PingService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import rc.playgrounds.config.ConfigModel
import kotlin.time.Duration

class StatusModel(
    private val scope: CoroutineScope,
    config: ConfigModel,
    private val steeringEventStream: SteeringEventStream,
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
            combine(steeringEventStream.events, _ping, ::asStatus).collect {
                _text.value = it
            }
        }
    }
}

private fun asStatus(event: SteeringEvent, ping: String): String {
    val control =
        "long: %.2f (raw: %.2f) ".format(event.long, event.rawLong) +
        "steer: %.2f (raw: %.2f) ".format(event.steer, event.rawSteer) +
        "pitch: %.2f (raw: %.2f) ".format(event.pitch, event.rawPitch) +
        "yaw: %.2f (raw: %.2f)".format(event.yaw, event.rawYaw)

    return ping + " " + control
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
                    statusReceiver("ping($server): ${duration.inWholeMilliseconds}ms")
                }
                .onFailure {
                    statusReceiver("ping($server): ${it.message ?: "(error)"} ${SAD_EMOJI.random()}")
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
)