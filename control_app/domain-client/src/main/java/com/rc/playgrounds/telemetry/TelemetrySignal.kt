package com.rc.playgrounds.telemetry

import com.rc.playgrounds.common.Clock

data class TelemetrySignal(
    val telemetry: Telemetry?,
    private val updatedAtMs: Long,
) {
    fun isUpToDate(clock: Clock): Boolean {
        if (telemetry == null) {
            return false
        }
        return clock.millis() - updatedAtMs <= TTL_MS
    }

    companion object {
        const val TTL_MS = 250L

        fun empty(): TelemetrySignal = TelemetrySignal(telemetry = null, updatedAtMs = 0L)
    }
}
