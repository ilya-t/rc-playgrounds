package com.rc.playgrounds.server

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class ServerViewModelProvider(
    camera: ServerCamera,
    private val configs: ServerActiveConfigProvider,
) {
    val viewModel: Flow<ServerViewModel> = combine(camera.state, configs.config) { state, config ->
        ServerViewModel(
            title = titleFor(state, config),
            target = "${config.host}:${config.port}",
            onTargetChanged = ::applyTarget,
        )
    }

    private fun applyTarget(raw: String) {
        val target = parseServerTarget(raw) ?: return
        configs.updateTarget(target.host, target.port)
    }

    private fun titleFor(state: ServerCameraState, config: ServerStreamConfig): String {
        return when (state) {
            ServerCameraState.Previewing -> "rc-server → ${config.host}:${config.port}"
            ServerCameraState.Stopped,
            ServerCameraState.Starting -> "rc-server"
            ServerCameraState.Missing -> "no external camera"
            ServerCameraState.CameraDenied -> "camera permission denied"
            ServerCameraState.Denied -> "usb permission denied"
            ServerCameraState.Failed -> "camera failed"
        }
    }
}
