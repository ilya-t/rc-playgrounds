package com.rc.playgrounds.control.steering

import android.graphics.PointF
import com.rc.playgrounds.config.ActiveConfigProvider
import com.rc.playgrounds.config.Config
import com.rc.playgrounds.config.model.ControlOffsets
import com.rc.playgrounds.config.model.ControlTuning
import com.rc.playgrounds.control.ControlInterpolation
import com.rc.playgrounds.control.ControlInterpolationProvider
import com.rc.playgrounds.control.gamepad.GamePadEventSessionProvider
import com.rc.playgrounds.control.gamepad.SessionGamepadEvent
import com.rc.playgrounds.control.longTrigger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue
import kotlin.math.sign

class SteerProvider(
    private val gamePadEventSessionProvider: GamePadEventSessionProvider,
    private val controlInterpolationProvider: ControlInterpolationProvider,
    private val activeConfigProvider: ActiveConfigProvider,
    private val scope: CoroutineScope
) {
    private val activeSteeringJob = MutableStateFlow<ActiveMode?>(null)
    private val steerState = MutableStateFlow(0f)
    private val steerWithOffsets = MutableStateFlow(0f)
    val steer: StateFlow<SteerValue> = steerWithOffsets

    init {
        scope.launch {
            activeConfigProvider.configFlow
                .map { it.controlTuning }
                .distinctUntilChanged()
                .collect { tuning ->
                    val mode = tuning.toMode()
                    activeSteeringJob.value?.job?.cancel()
                    activeSteeringJob.value = ActiveMode(
                        mode = mode,
                        job = when (mode) {
                            StreeringMode.WHEEL_EMULATION -> wheelEmulatedSteering(tuning)
                            StreeringMode.LIMITING_BY_TRIGGER -> steerLimitingByLongTrigger()
                        }
                    )
                }

        }

        scope.launch {
            combine(
                activeConfigProvider.configFlow,
                steerState,
            ) { config: Config, steer: Float ->
                val offsets: ControlOffsets = config.controlOffsets
                val steerWithOffsets: Float = steer + offsets.steer
                val steerZone: PointF? = config.controlTuning.steerZone

                if (steerZone != null) {
                    val limitedSteer = steerWithOffsets.absoluteValue.coerceIn(
                        steerZone.x,
                        steerZone.y
                    ) * steerWithOffsets.sign
                    limitedSteer
                } else {
                    steerWithOffsets
                }
            }.collect {
                steerWithOffsets.value = it.trim()
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun wheelEmulatedSteering(tuning: ControlTuning): Job {
        // Wheel emulation engine lives in a separate private class (below)
        val wheelConfig = tuning.wheel
        val wheel = WheelEmulator(
            maxWheelAngleDeg = wheelConfig?.maxAngleDeg ?: 28f,
            maxTurnRateDegPerSec = wheelConfig?.maxTurnRateDegPerSec ?: 420f,
            centerReturnRateDegPerSec = wheelConfig?.centerReturnRateDegPerSec ?: 140f,
            deadzone = wheelConfig?.deadzone ?: 0.06f,
            curveBlend = wheelConfig?.curveBlend ?: 0.55f,
            emaCutoffHz = wheelConfig?.emaCutoffHz ?: 10f,
            centerStickThreshold = wheelConfig?.centerStickThreshold ?: 0.02f,
            damping = wheelConfig?.damping ?: 0.9f,
        )
        return scope.launch(Dispatchers.IO.limitedParallelism(1)) {
            while (isActive) {
                val event: SessionGamepadEvent? = gamePadEventSessionProvider.lastEvent
                if (event == null) {
                    delay(8L)
                    continue
                }
                val raw = event.event.leftStickX
                val normWheel = wheel.step(raw)          // [-1..1]
                steerState.value = normWheel                // publish emulated wheel
                delay(16L)                              // ~60 Hz (dt handled inside)
            }
        }
    }

    private fun steerLimitingByLongTrigger(): Job {
        return scope.launch {
            combine(
                gamePadEventSessionProvider.events,
                controlInterpolationProvider.interpolation,
            ) { sessionEvent: SessionGamepadEvent,
                interpolation: ControlInterpolation ->
                val steerAtStart = sessionEvent.sessionStart?.leftStickX
                val rawSteer = sessionEvent.event.leftStickX

                interpolation.fixSteer(
                    rawSteer,
                    activeTrigger = sessionEvent.event.longTrigger,
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
    WHEEL_EMULATION,
    LIMITING_BY_TRIGGER,
}

private fun ControlTuning.toMode(): StreeringMode {
    return when (this.steerMode) {
        "steer_limit_at_trigger" -> StreeringMode.LIMITING_BY_TRIGGER
        "wheel" -> StreeringMode.WHEEL_EMULATION
        else -> StreeringMode.LIMITING_BY_TRIGGER
    }
}
