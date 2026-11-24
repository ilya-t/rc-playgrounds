package com.rc.playgrounds.telemetry

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class TelemetryProvider {
    private val states = MutableStateFlow<Telemetry?>(null)
    val state: StateFlow<Telemetry?> = states

    fun update(value: Telemetry) {
        states.value = value
    }
}
