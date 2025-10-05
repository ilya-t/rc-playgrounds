package com.rc.playgrounds.control

import com.rc.playgrounds.config.ActiveConfigProvider
import com.rc.playgrounds.config.model.ControlTuning
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class ControlTuningProvider(
    private val activeConfigProvider: ActiveConfigProvider,
    private val scope: CoroutineScope,
) {
    private val activeProfile = MutableStateFlow<String?>(null)
    val activeControlProfile: Flow<String?> = activeProfile

    val controlTuning: Flow<ControlTuning> = combine(
        activeConfigProvider.configFlow.map { it.env }.distinctUntilChanged(),
        activeConfigProvider.configFlow.map { it.controlProfiles }.distinctUntilChanged(),
        activeControlProfile
    ) { env: Map<String,String>, profiles: List<ControlTuning>, activeProfileName: String? ->
        if (activeProfileName == null) {
            return@combine profiles.firstOrNull() ?: ControlTuning()
        }

        buildProfile(activeProfileName, profiles)
    }

    init {
        scope.launch {
            val initial = activeConfigProvider.configFlow.first().controlProfiles.firstOrNull()?.name
            activeProfile.compareAndSet(null, initial)
        }
    }

    fun changeProfile(name: String) {
        activeProfile.value = name
    }
}

private fun buildProfile(
    name: String,
    profiles: List<ControlTuning>
): ControlTuning {
    profiles.firstOrNull() ?: return ControlTuning()
    return profiles.fold(ControlTuning()) { acc, incoming ->

        val result = ControlTuning(
            name = incoming.name,
            pitchFactor = incoming.pitchFactor ?: acc.pitchFactor,
            rawPitchZone = incoming.rawPitchZone ?: acc.rawPitchZone,
            yawFactor = incoming.yawFactor ?: acc.yawFactor,
            rawYawZone = incoming.rawYawZone ?: acc.rawYawZone,
            rawSteerZone = incoming.rawSteerZone ?: acc.rawSteerZone,
            steerMode = incoming.steerMode ?: acc.steerMode,
            steerExponentFactor = incoming.steerExponentFactor ?: acc.steerExponentFactor,
            rawSteerLimitAtTrigger = incoming.rawSteerLimitAtTrigger ?: acc.rawSteerLimitAtTrigger,
            rawForwardLongZones = incoming.rawForwardLongZones ?: acc.rawForwardLongZones,
            rawBackwardLongZones = incoming.rawBackwardLongZones ?: acc.rawBackwardLongZones,
            wheel = incoming.wheel ?: acc.wheel,
        )




        if (name == incoming.name) {
            return result
        }
        result
    }
}

