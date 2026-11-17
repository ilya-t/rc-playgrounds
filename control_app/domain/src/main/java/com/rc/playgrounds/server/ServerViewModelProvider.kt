package com.rc.playgrounds.server

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ServerViewModelProvider(
    camera: ServerCamera,
) {
    val viewModel: Flow<ServerViewModel> = camera.state.map { state ->
        ServerViewModel(title = titleFor(state))
    }

    private fun titleFor(state: ServerCameraState): String {
        return when (state) {
            ServerCameraState.Stopped,
            ServerCameraState.Starting,
            ServerCameraState.Previewing -> "rc-server"
            ServerCameraState.Missing -> "no external camera"
            ServerCameraState.CameraDenied -> "camera permission denied"
            ServerCameraState.Denied -> "usb permission denied"
            ServerCameraState.Failed -> "camera failed"
        }
    }
}
