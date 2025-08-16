package com.rc.playgrounds.control.steering

import com.rc.playgrounds.config.ActiveConfigProvider
import com.rc.playgrounds.config.model.ControlOffsets
import com.rc.playgrounds.control.ControlInterpolation
import com.rc.playgrounds.control.ControlInterpolationProvider
import com.rc.playgrounds.control.gamepad.GamePadEventSessionProvider
import com.rc.playgrounds.control.gamepad.SessionGamepadEvent
import com.rc.playgrounds.control.longTrigger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class SteerProvider(
    private val gamePadEventSessionProvider: GamePadEventSessionProvider,
    private val controlInterpolationProvider: ControlInterpolationProvider,
    private val activeConfigProvider: ActiveConfigProvider,
    scope: CoroutineScope
) {
    private val _state = MutableStateFlow(0f)
    val steer: StateFlow<SteerValue> = _state

    init {
        scope.launch {
            combine(
                gamePadEventSessionProvider.events,
                activeConfigProvider.configFlow.map { it.controlOffsets },
                controlInterpolationProvider.interpolation,
            ) { sessionEvent: SessionGamepadEvent,
                offsets: ControlOffsets,
                interpolation: ControlInterpolation ->
                val steerAtStart = sessionEvent.sessionStart?.leftStickX
                val rawSteer = sessionEvent.event.leftStickX

                val fixedSteer = interpolation.fixSteer(
                    rawSteer,
                    activeTrigger = sessionEvent.event.longTrigger,
                    steerAtStart = steerAtStart,
                ) + offsets.steer
                fixedSteer.trim()
            }.collect {
                _state.value = it
            }
        }
    }
}