package com.rc.playgrounds.remote.stream

import com.rc.playgrounds.config.ActiveConfigProvider
import com.rc.playgrounds.config.AdaptiveRemoteCmd
import com.rc.playgrounds.config.model.NetworkTarget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class RemoteStreamConfigController(
    private val config: ActiveConfigProvider,
    private val qualityResolver: StreamQualityProvider,
    private val scope: CoroutineScope,
) {
    private val _state = MutableStateFlow<RemoteStreamConfig?>(null)
    val state: Flow<RemoteStreamConfig?> = _state

    init {
        scope.launch {
            combine(
                config.configFlow.map { it.adaptiveRemoteCmd },
                config.configFlow.map { it.streamTarget },
                qualityResolver.currentQuality,
                ::asRemoteStreamConfig
            ).collect {
                _state.value = it
            }
        }
    }

    private fun asRemoteStreamConfig(
        adaptiveRemoteCmd: AdaptiveRemoteCmd?,
        streamTarget: NetworkTarget?,
        parameters: StreamParameters,
        ): RemoteStreamConfig? {
        if (adaptiveRemoteCmd == null) {
            return null
        }

        if (!adaptiveRemoteCmd.enabled) {
            return null
        }

        if (streamTarget == null) {
            return null
        }

        return RemoteStreamConfig(
            parameters,
            remoteCmd = buildRemoteCmd(
                template = adaptiveRemoteCmd.cmdTemplate,
                parameters = parameters,
                server = streamTarget,
            )
        )
    }

    private fun buildRemoteCmd(template: String,
                               parameters: StreamParameters,
                               server: NetworkTarget,
                               ): String {
        return template
            .replace("@{width}", parameters.width.toString())
            .replace("@{height}", parameters.height.toString())
            .replace("@{framerate}", parameters.framerate.toString())
            .replace("@{bitrate}", parameters.bitrate.toString())
            .replace("@{stream_target}", server.address)
            .replace("@{stream_target_port}", server.port.toString())
    }
}