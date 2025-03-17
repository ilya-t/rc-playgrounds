package com.rc.playgrounds.status.view

import com.rc.playgrounds.status.PingService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import rc.playgrounds.config.ConfigModel
import kotlin.time.Duration

class StatusModel(
    private val scope: CoroutineScope,
    config: ConfigModel,
) {

    private val pingTarget: Flow<String?> = config.configFlow.map { it.controlServer?.address }
    private val _text = MutableStateFlow("(?)")
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
                    _text.value = it
                }
            }
        }
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
                    statusReceiver("ping($server): ${duration.inWholeMilliseconds}ms")
                }
                .onFailure {
                    statusReceiver("ping($server): ${it.message ?: "(error)"}")
                }
        }
    }

    fun close() {
        pingServiceJob.cancel()
        statusJob.cancel()
    }
}
