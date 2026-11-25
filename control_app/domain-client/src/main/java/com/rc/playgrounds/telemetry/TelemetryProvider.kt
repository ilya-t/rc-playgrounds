package com.rc.playgrounds.telemetry

import com.rc.playgrounds.common.Clock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class TelemetryProvider(
    private val clock: Clock,
) {
    private val states = MutableStateFlow(TelemetrySignal.empty())

    fun getState(): StateFlow<TelemetrySignal> = states

    fun update(value: Telemetry) {
        states.value = TelemetrySignal(telemetry = value, updatedAtMs = clock.millis())
    }
}
