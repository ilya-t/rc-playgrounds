package com.rc.playgrounds.control.steering

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sign

/**
 * Emulates a car-like steering wheel using a gamepad stick input.
 * Converts [-1..1] stick values into a smooth wheel angle that feels
 * weighted, recenters naturally, and avoids jitter.
 */
internal class WheelEmulator(
    /**
     * Maximum wheel lock angle in degrees.
     * - Defines how far the emulated steering wheel can turn left/right.
     * - Affects sensitivity: lower = more stable but less agile, higher = twitchier.
     * - Typical values: 24–34° for cars, up to 45° for karts.
     */
    private val maxWheelAngleDeg: Float = 28f,

    /**
     * Maximum wheel turning speed at full stick deflection (degrees per second).
     * - Controls how fast the virtual wheel moves when the stick is fully held.
     * - Higher = snappier response; lower = smoother but lazier steering.
     * - Typical range: 300–600 deg/s.
     */
    private val maxTurnRateDegPerSec: Float = 420f,

    /**
     * Auto-centering speed when stick is near zero (degrees per second).
     * - Governs how quickly the wheel recenters itself when the stick is released.
     * - Higher = stronger “self-aligning torque” feel; lower = sluggish return.
     * - Typical range: 80–200 deg/s.
     */
    private val centerReturnRateDegPerSec: Float = 140f,

    /**
     * Radial deadzone for stick input (0..1).
     * - Ignores small stick movements around the center to avoid noise/drift.
     * - Too high → feels unresponsive near center.
     * - Too low → wheel twitches from tiny stick jitters.
     * - Typical range: 0.05–0.1.
     */
    private val deadzone: Float = 0.06f,

    /**
     * Response curve blend factor (0=linear, 1=cubic).
     * - Shapes the stick response:
     *   0.0 → linear, direct mapping.
     *   1.0 → cubic, softer near center, sharper near edges.
     * - Good middle ground is 0.5–0.6 for precision + agility.
     */
    private val curveBlend: Float = 0.55f,

    /**
     * Low-pass filter cutoff frequency for stick input (Hz).
     * - Smooths out jittery inputs.
     * - Higher values = more responsive but noisier.
     * - Lower values = smoother but laggier.
     * - Typical range: 8–15 Hz.
     */
    private val emaCutoffHz: Float = 10f,

    /**
     * Stick threshold near zero where auto-centering kicks in.
     * - If stick magnitude < this, the wheel starts pulling back to center.
     * - Larger threshold = wheel recenters more often, even with slight input.
     * - Smaller threshold = only recenters when stick is really centered.
     * - Typical range: 0.01–0.05.
     */
    private val centerStickThreshold: Float = 0.02f,

    /**
     * Damping factor (0..1) applied to wheel angular velocity.
     * - Simulates steering “weight” by slowing down wheel changes slightly.
     * - 1.0 = no damping (very loose).
     * - 0.8–0.95 = mild damping (realistic feel).
     * - Too low (<0.7) makes steering sluggish and unresponsive.
     */
    private val damping: Float = 0.9f,
) {
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
        val ax = abs(x)
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