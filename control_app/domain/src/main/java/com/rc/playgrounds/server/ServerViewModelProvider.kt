package com.rc.playgrounds.server

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.util.Locale

class ServerViewModelProvider(
    camera: ServerCamera,
    controls: Flow<ServerControlState>,
    videoTarget: Flow<VideoTarget?>,
) {
    val viewModel: Flow<ServerViewModel> = combine(
        camera.state,
        controls,
        videoTarget,
    ) { state, control, target ->
        ServerViewModel(
            title = titleFor(state, target),
            controls = controlsText(control),
        )
    }
}

private fun titleFor(state: ServerCameraState, target: VideoTarget?): String {
    return when (state) {
        ServerCameraState.Previewing -> {
            if (target == null) {
                "rc-server"
            } else {
                "rc-server → ${target.host}:${target.port}"
            }
        }
        ServerCameraState.Stopped,
        ServerCameraState.Starting -> "rc-server"
        ServerCameraState.Missing -> "no external camera"
        ServerCameraState.CameraDenied -> "camera permission denied"
        ServerCameraState.Denied -> "usb permission denied"
        ServerCameraState.Failed -> "camera failed"
    }
}

private fun controlsText(control: ServerControlState): String {
    if (!control.live) {
        return "no control"
    }
    val axes = String.format(
        Locale.US,
        "yaw %.2f  pitch %.2f\nsteer %.2f  long %.2f",
        control.yaw,
        control.pitch,
        control.steer,
        control.long,
    )
    if (control.streamCmdHash.isEmpty()) {
        return axes
    }
    return "$axes\n${control.streamCmdHash}"
}
