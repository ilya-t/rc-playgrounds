package com.rc.playgrounds.control

import com.rc.playgrounds.config.ActiveConfigProvider
import com.rc.playgrounds.config.model.ControlTuning
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ControlTuningProvider(
    private val activeConfigProvider: ActiveConfigProvider,
) {
    val controlTuning: Flow<ControlTuning> = activeConfigProvider.configFlow.map { it.controlTuning }
}

