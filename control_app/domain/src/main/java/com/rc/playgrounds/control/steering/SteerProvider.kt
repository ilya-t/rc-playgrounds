package com.rc.playgrounds.control.steering

import com.rc.playgrounds.config.ActiveConfigProvider
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
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sign

class SteerProvider(
    private val gamePadEventSessionProvider: GamePadEventSessionProvider,
    private val controlInterpolationProvider: ControlInterpolationProvider,
    private val activeConfigProvider: ActiveConfigProvider,
    private val scope: CoroutineScope
) {
    private val activeSteeringJob = MutableStateFlow<ActiveMode?>(null)
    private val _state = MutableStateFlow(0f)
    val steer: StateFlow<SteerValue> = _state

    init {
        scope.launch {
            activeConfigProvider.configFlow
                .map { it.controlTuning.toMode() }
                .distinctUntilChanged()
                .collect { m ->
                    if (m == activeSteeringJob.value?.mode) {
                        return@collect
                    }
                    activeSteeringJob.value?.job?.cancel()
                    activeSteeringJob.value = ActiveMode(
                        mode = m,
                        job = when (m) {
                            StreeringMode.WHEEL_EMULATION -> wheelEmulatedSteering()
                            StreeringMode.LIMITING_BY_TRIGGER -> steerLimitingByLongTrigger()
                        }
                    )
                }

        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun wheelEmulatedSteering(): Job {
        // Wheel emulation engine lives in a separate private class (below)
        val wheel = WheelEmulator()
        return scope.launch(Dispatchers.IO.limitedParallelism(1)) {
            while (isActive) {
                val event: SessionGamepadEvent? = gamePadEventSessionProvider.lastEvent
                if (event == null) {
                    delay(8L)
                    continue
                }
                val raw = event.event.leftStickX
                val normWheel = wheel.step(raw)          // [-1..1]
                _state.value = normWheel                // publish emulated wheel
                delay(16L)                              // ~60 Hz (dt handled inside)
            }
        }
    }

    private fun steerLimitingByLongTrigger(): Job {
        return scope.launch {
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

private class WheelEmulator {
    // --- Tunables (adjust to taste) ---
    private val maxWheelAngleDeg = 28f               // steering lock (deg)
    private val maxTurnRateDegPerSec = 420f          // wheel speed at full stick
    private val centerReturnRateDegPerSec = 140f     // auto-center when stick ~0
    private val deadzone = 0.06f                     // radial deadzone
    private val curveBlend = 0.55f                   // 0=linear, 1=x^3
    private val emaCutoffHz = 10f                    // input smoothing cutoff
    private val centerStickThreshold = 0.02f         // start recentre below |x|
    private val damping = 0.9f                       // light viscous damping

    // --- State ---
    private var wheelDeg = 0f
    private var lastNs: Long = System.nanoTime()
    private var emaInitialized = false
    private var emaY = 0f

    /** Call once per frame. Returns normalized wheel angle in [-1..1]. */
    fun step(rawStickValue: Float): Float {
        // dt from monotonic time
        val now = System.nanoTime()
        var dt = (now - lastNs) / 1_000_000_000.0f
        lastNs = now
        dt = dt.coerceIn(0f, 0.1f)

        // 1) Preprocess: clamp -> deadzone -> curve -> EMA
        var x = rawStickValue.coerceIn(-1f, 1f)
        x = applyDeadzoneRadial(x, deadzone)
        x = cubicBlend(x, curveBlend)
        x = ema(x, dt, emaCutoffHz)

        // 2) Rate control (stick drives wheel angular velocity)
        var rate = x * maxTurnRateDegPerSec

        // 3) Auto-center if stick ~ 0
        if (abs(x) < centerStickThreshold) {
            val norm = (wheelDeg / maxWheelAngleDeg).coerceIn(-1f, 1f)
            rate += (-norm) * centerReturnRateDegPerSec
        }

        // 4) Light damping for weighted feel
        rate *= (1f - (1f - damping) * dt * 60f).coerceIn(0.7f, 1f)

        // 5) Integrate & clamp to lock
        wheelDeg = (wheelDeg + rate * dt).coerceIn(-maxWheelAngleDeg, maxWheelAngleDeg)

        // 6) Output normalized wheel
        return (wheelDeg / maxWheelAngleDeg).coerceIn(-1f, 1f)
    }

    fun reset(angleDeg: Float = 0f) {
        wheelDeg = angleDeg.coerceIn(-maxWheelAngleDeg, maxWheelAngleDeg)
        emaInitialized = false
        emaY = 0f
        lastNs = System.nanoTime()
    }

    // --- Helpers ---

    private fun applyDeadzoneRadial(x: Float, dz: Float): Float {
        val ax = kotlin.math.abs(x)
        if (ax <= dz) return 0f
        val scaled = (ax - dz) / (1f - dz)
        return sign(x) * scaled.coerceIn(0f, 1f)
    }

    // x' = (1-a)*x + a*x^3 (precise near center, faster near edges)
    private fun cubicBlend(x: Float, a: Float): Float {
        return (1f - a) * x + a * x * x * x
    }

    // Time-correct EMA from cutoff (Hz)
    private fun ema(x: Float, dt: Float, cutoffHz: Float): Float {
        val rc = 1f / (2f * PI.toFloat() * cutoffHz)
        val alpha = if (dt <= 0f) 1f else (dt / (rc + dt)).coerceIn(0f, 1f)
        emaY = if (!emaInitialized) {
            emaInitialized = true; x
        } else (1f - alpha) * emaY + alpha * x
        return emaY
    }
}
