package com.rc.playgrounds.server

import com.rc.playgrounds.server.controllink.MicroviewLinkState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.scan
import java.util.Locale

class ServerViewModelProvider(
    camera: ServerCamera,
    controls: Flow<ServerControlState>,
    videoTarget: Flow<VideoTarget?>,
    linkState: Flow<MicroviewLinkState>,
    problems: Flow<Error>,
) {
    private val problemLines: Flow<List<Error>> = problems
        .scan(emptyList<Error>()) { lines, line ->
            (lines + line).takeLast(PROBLEM_LINES)
        }
        .onStart { emit(emptyList()) }

    val viewModel: Flow<ServerViewModel> = combine(
        camera.state,
        controls,
        videoTarget,
        linkState,
        problemLines,
    ) { state, control, target, link, reported ->
        ServerViewModel(
            title = titleFor(state, target),
            controls = controlsText(control, link, reported),
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

private fun controlsText(
    control: ServerControlState,
    link: MicroviewLinkState,
    problems: List<Error>,
): String {
    val axes = if (!control.live) {
        "no control"
    } else {
        val text = String.format(
            Locale.US,
            "yaw %.2f  pitch %.2f\nsteer %.2f  long %.2f",
            control.yaw,
            control.pitch,
            control.steer,
            control.long,
        )
        if (control.streamCmdHash.isEmpty()) text else "$text\n${control.streamCmdHash}"
    }
    val status = "$axes\n${uartText(link)}"
    if (problems.isEmpty()) {
        return status
    }
    return "$status\n${problems.joinToString("\n", transform = ::problemLine)}"
}

private fun problemLine(reported: Error): String {
    val detail = reported.error.message?.takeIf { it.isNotEmpty() }
        ?: reported.error.javaClass.simpleName
    return "${reported.service}: $detail"
}

private fun uartText(link: MicroviewLinkState): String {
    return when (link) {
        MicroviewLinkState.NoAdapter -> "uart: no adapter"
        MicroviewLinkState.NoPermission -> "uart: no permission"
        MicroviewLinkState.Open -> "uart: open"
        MicroviewLinkState.Failsafe -> "uart: failsafe"
    }
}

private const val PROBLEM_LINES = 8
