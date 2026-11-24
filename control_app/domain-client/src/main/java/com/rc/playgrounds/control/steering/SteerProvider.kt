package com.rc.playgrounds.control.steering

import android.graphics.PointF
import com.rc.playgrounds.config.ActiveConfigProvider
import com.rc.playgrounds.config.Config
import com.rc.playgrounds.config.model.ControlOffsets
import com.rc.playgrounds.config.model.gamepad.GamepadMapping
import com.rc.playgrounds.control.ControlInterpolation
import com.rc.playgrounds.control.ControlInterpolationProvider
import com.rc.playgrounds.control.ControlTuning
import com.rc.playgrounds.control.ControlTuningProvider
import com.rc.playgrounds.control.gamepad.GamePadEventSessionProvider
import com.rc.playgrounds.control.gamepad.SessionGamepadEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue
import kotlin.math.pow
import kotlin.math.sign

class SteerProvider(
    private val gamePadEventSessionProvider: GamePadEventSessionProvider,
    private val controlInterpolationProvider: ControlInterpolationProvider,
    private val activeConfigProvider: ActiveConfigProvider,
    private val controlTuningProvider: ControlTuningProvider,
    private val scope: CoroutineScope
) {
    private val activeSteeringJob = MutableStateFlow<ActiveMode?>(null)
    private val steerState = MutableStateFlow(0f)
    private val steerWithOffsets = MutableStateFlow(SteeringValues(0f, 0f))
    val steer: StateFlow<SteeringValues> = steerWithOffsets

    init {
        scope.launch {
            controlTuningProvider.controlTuning
                .distinctUntilChanged()
                .collect { tuning ->
                    val mode = tuning.toMode()
                    activeSteeringJob.value?.job?.cancel()
                    activeSteeringJob.value = ActiveMode(
                        mode = mode,
                        job = when (mode) {
                            StreeringMode.LIMITING_BY_TRIGGER -> steerLimitingByLongTrigger()
                            StreeringMode.EXPONENT -> steerByExponent()
                        }
                    )
                }

        }

        scope.launch {
            combine(
                activeConfigProvider.configFlow,
                controlTuningProvider.controlTuning,
                steerState,
            ) { c: Config, tuning: ControlTuning, steer: Float ->
                val offsets: ControlOffsets = c.controlOffsets
                val steerWithOffsets: Float = steer + offsets.steer
                val steerZone: PointF? = tuning.steerZone

                val value = if (steerZone != null) {
                    val limitedSteer = steerWithOffsets.coerceIn(
                        steerZone.x,
                        steerZone.y
                    )
                    limitedSteer
                } else {
                    steerWithOffsets
                }

                SteeringValues(
                    value = value.trim(),
                    rawValue = steer,
                )
            }.collect {
                steerWithOffsets.value = it
            }
        }
    }

    private fun steerByExponent(): Job {
        return scope.launch {
            combine(
                gamePadEventSessionProvider.events,
                controlTuningProvider.controlTuning.map { it.steerExponentFactor?.toDouble() },
                activeConfigProvider.configFlow.map { it.gamepadMapping },
            ) { sessionEvent: SessionGamepadEvent,
                exponentFactor: Double?,
                gamepadMapping: GamepadMapping ->
                val rawSteer = gamepadMapping.steer.resolveAxis(sessionEvent.event).toDouble()
                expo(x = rawSteer, exponentFactor ?: 2.0).toFloat()
            }.collect {
                steerState.value = it
            }
        }
    }

    private fun expo(x: Double, factor: Double = 2.0): Double {
        val a = x.absoluteValue.coerceIn(0.0, 1.0)
        return x.sign * a.pow(factor)
    }

    private fun steerLimitingByLongTrigger(): Job {
        return scope.launch {
            combine(
                gamePadEventSessionProvider.events,
                controlInterpolationProvider.interpolation,
                activeConfigProvider.configFlow.map { it.gamepadMapping },
            ) { sessionEvent: SessionGamepadEvent,
                interpolation: ControlInterpolation,
                gamepadMapping: GamepadMapping ->
                val steerAtStart: Float? =
                    sessionEvent.sessionStart?.let { gamepadMapping.steer.resolveAxis(it) }
                val rawSteer = gamepadMapping.steer.resolveAxis(sessionEvent.event)

                interpolation.fixSteer(
                    rawSteer,
                    activeTrigger = gamepadMapping.long.resolveAxis(sessionEvent.event),
                    steerAtStart = steerAtStart,
                )
            }.collect {
                steerState.value = it
            }
        }
    }
}

private class ActiveMode(
    val mode: StreeringMode,
    val job: Job,
)

private enum class StreeringMode {
    LIMITING_BY_TRIGGER,
    EXPONENT,
}

private fun ControlTuning.toMode(): StreeringMode {
    return when (this.steerMode) {
        "steer_limit_at_trigger" -> StreeringMode.LIMITING_BY_TRIGGER
        "exponent" -> StreeringMode.EXPONENT
        else -> StreeringMode.LIMITING_BY_TRIGGER
    }
}

class SteeringValues(
    val value: SteerValue,
    val rawValue: SteerValue,
)