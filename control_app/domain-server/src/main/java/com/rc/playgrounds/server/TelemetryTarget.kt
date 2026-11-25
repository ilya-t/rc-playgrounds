package com.rc.playgrounds.server

import com.rc.playgrounds.common.ControlSignal

data class TelemetryTarget(
    val host: String,
    val port: Int,
)

fun parseTelemetryTarget(signal: ControlSignal): TelemetryTarget? {
    val port = signal.telemetryPort ?: return null
    if (signal.clientAddr.isEmpty()) {
        return null
    }
    return TelemetryTarget(signal.clientAddr, port)
}
