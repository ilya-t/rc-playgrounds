package com.rc.playgrounds.remote.stream

import com.rc.playgrounds.config.ActiveConfigProvider
import com.rc.playgrounds.control.gamepad.GamepadButtonPress
import com.rc.playgrounds.control.gamepad.GamepadEventStream
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class StreamQualityProvider(
    private val activeConfigProvider: ActiveConfigProvider,
    private val eventStream: GamepadEventStream,
    private val scope: CoroutineScope,
) {
    private val _quality = MutableStateFlow(QualityProfile.DEFAULT_PROFILES.first())
    private var profileIndex = 0

    val currentQuality: Flow<QualityProfile> = _quality

    init {
        scope.launch {
            combine(
                activeConfigProvider.configFlow.map { it.stream.qualityProfiles },
                eventStream.buttonEvents,
            ) { a, b -> a to b }
            .collect { (profiles, event) ->
                when (event) {
                    GamepadButtonPress.DpadDown -> {
                        profileIndex--
                        invalidate(profiles)
                    }
                    GamepadButtonPress.DpadUp -> {
                        profileIndex++
                        invalidate(profiles)
                    }
                }
            }
        }
    }

    private fun invalidate(qualityProfiles: List<QualityProfile>) {
        val current = qualityProfiles.getOrNull(profileIndex)

        if (current != null) {
            _quality.value = current
            return
        }

        if (profileIndex < 0) {
            profileIndex = 0
            invalidate(QualityProfile.DEFAULT_PROFILES)
            return
        }

        profileIndex = qualityProfiles.lastIndex
        invalidate(QualityProfile.DEFAULT_PROFILES)
    }
}