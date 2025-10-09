package com.rc.playgrounds.control

import com.rc.playgrounds.config.ActiveConfigProvider
import com.rc.playgrounds.config.Config
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch

class ControlTuningProvider(
    private val activeConfigProvider: ActiveConfigProvider,
    private val scope: CoroutineScope,
) {
    private val _controlTuning = MutableStateFlow<ControlTuning?>(null)
    val controlTuning: Flow<ControlTuning> = _controlTuning.filterNotNull()

    init {
        scope.launch {
            activeConfigProvider.configFlow.collect { c: Config ->
                val new = ControlTuning(
                    steerMode = c.controlTuning.steerMode(c.env),
                    pitchFactor = c.controlTuning.pitchFactor(c.env),
                    yawFactor = c.controlTuning.yawFactor(c.env),
                    pitchZone = c.controlTuning.pitchZone(c.env),
                    yawZone = c.controlTuning.yawZone(c.env),
                    steerZone = c.controlTuning.steerZone(c.env),
                    steerExponentFactor = c.controlTuning.steerExponentFactor(c.env),
                    forwardLongZones = c.controlTuning.forwardLongZones(c.env),
                    backwardLongZones = c.controlTuning.backwardLongZones(c.env),
                    steerLimitAtTrigger = c.controlTuning.steerLimitAtTrigger(c.env),
                    wheel = c.controlTuning.wheel,
                )

                _controlTuning.value = new
            }
        }
    }
}
