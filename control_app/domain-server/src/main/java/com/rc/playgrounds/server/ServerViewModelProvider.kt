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
    telemetryTarget: Flow<TelemetryTarget?>,
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
        ServerSnapshot(state, control, target, link, reported)
    }.combine(telemetryTarget) { snapshot, telemetry ->
        ServerViewModel(
            title = titleFor(snapshot.state, snapshot.video),
            controls = controlsText(snapshot.control, snapshot.link, snapshot.problems, telemetry),
        )
    }
}

private data class ServerSnapshot(
    val state: ServerCameraState,
    val control: ServerControlState,
    val video: VideoTarget?,
    val link: MicroviewLinkState,
    val problems: List<Error>,
)

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
    telemetry: TelemetryTarget?,
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
    val status = "$axes\n${uartText(link)}\n${telemetryText(telemetry)}"
    if (problems.isEmpty()) {
        return status
    }
    return "$status\n${problems.joinToString("\n", transform = ::problemLine)}"
}

private fun telemetryText(target: TelemetryTarget?): String {
    return if (target == null) {
        "telemetry: —"
    } else {
        "telemetry → ${target.host}:${target.port}"
    }
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
