package com.rc.playgrounds.control

import android.graphics.PointF
import android.view.animation.AccelerateInterpolator
import com.rc.playgrounds.config.ActiveConfigProvider
import com.rc.playgrounds.config.Config
import com.rc.playgrounds.config.model.ControlTuning
import com.rc.playgrounds.config.model.MappingZone
import com.rc.playgrounds.control.steering.SteerValue
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlin.math.absoluteValue
import kotlin.math.sign
import kotlin.math.withSign

class ControlInterpolationProvider(
    private val controlTuningProvider: ControlTuningProvider,
    private val activeConfigProvider: ActiveConfigProvider,
) {
    val interpolation: Flow<ControlInterpolation> = combine(
        activeConfigProvider.configFlow,
        controlTuningProvider.controlTuning,
    ) { config, tuning ->
        tuning.asInterpolation(config)
    }
}

class ControlInterpolation(
    private val pitch: AccelerateInterpolator?,
    private val pitchTranslator: (Float) -> Float,
    private val yaw: AccelerateInterpolator?,
    private val yawTranslator: (Float) -> Float,
    private val steerTranslator: (steer: Float, trigger: Float) -> Float,
    private val longTranslator: (Float) -> Float,
) {
    fun fixPitch(value: Float): Float {
        return fix(pitch, pitchTranslator, value)
    }

    fun fixYaw(value: Float): Float {
        return fix(yaw, yawTranslator, value)
    }

    fun fixSteer(steer: Float, activeTrigger: Float, steerAtStart: Float?): SteerValue {
        val fixedSteer: Float = steerTranslator(steer, activeTrigger)

        if (steerAtStart == null) {
            return fixedSteer
        }

        if (fixedSteer == steer) {
            return fixedSteer
        }

        if (steer > 0 && fixedSteer < steerAtStart.absoluteValue) {
            // Decline steer fix (right)
            return steer
        }

        if (steer < 0 && fixedSteer > -1*steerAtStart.absoluteValue) {
            // Decline steer fix (left)
            return steer
        }

        return fixedSteer
    }

    fun fixLong(value: Float): Float {
        return longTranslator(value)
    }

    private fun fix(
        interpolator: AccelerateInterpolator?,
        translator: (Float) -> Float,
        value: Float
    ): Float {
        val translatedValue = translator(value)
        if (interpolator == null) {
            return translatedValue
        }

        val sign = sign(translatedValue)
        val interpolated = interpolator.getInterpolation(translatedValue.absoluteValue)
        return interpolated * sign
    }
}

private fun ControlTuning.asInterpolation(c: Config) = ControlInterpolation(
    pitch = create(pitchFactor(c.env)),
    pitchTranslator = create(pitchZone(c.env)),
    yaw = create(yawFactor(c.env)),
    yawTranslator = create(yawZone(c.env)),
    steerTranslator = { steer: Float, rawTrigger: Float ->
        val trigger = rawTrigger.absoluteValue
        steerLimitAtTrigger
            .find { trigger >= it.src.x && trigger <= it.src.y }
            ?.dst
            ?.let { limits ->
                val maxSteer = translate(trigger, 0f, 1f, limits.x, limits.y)
                translate(steer.absoluteValue, 0f, 1f, 0f, maxSteer) * steer.sign
            }

            ?: steerZone(c.env)?.let {
                translate(steer.absoluteValue, 0f, 1f, it.x, it.y) * steer.sign
            }

            ?: steer
    },
    longTranslator = if (forwardLongZones.isNotEmpty()) {
        val zonesNegative = backwardLongZones.ifEmpty { forwardLongZones }
/*
        val zonesNegative = backwardLongZones.ifEmpty { forwardLongZones }.map {
            NO LUCK
            MappingZone(
                src = PointF().apply {
                    x = -it.src.x.absoluteValue
                    y = -it.src.y.absoluteValue
                },
                dst = PointF().apply {
                    x = -it.dst.x.absoluteValue
                    y = -it.dst.y.absoluteValue
                },
            )
        }
*/        create(negative = zonesNegative, positive = forwardLongZones)
    } else {
        create(PointF(0f, 1f))
    },
)


private fun create(negative: List<MappingZone>, positive: List<MappingZone>): (Float) -> Float {
    return { input ->
        val zones = if (input >= 0) {
            positive
        } else {
            negative
        }
        val absInput = input.absoluteValue
        val targetZone: MappingZone? = zones
            .find { absInput >= it.src.x && absInput <= it.src.y }
            ?: zones.lastOrNull()

        targetZone
            ?.let {
                translate(absInput, it.src.x, it.src.y, it.dst.x, it.dst.y)
            }
            ?.withSign(input)
            ?: input
    }
}


private fun create(factor: Float?): AccelerateInterpolator? {
    if (factor == null) {
        return null
    }

    if (factor.isNaN()) {
        return null
    }

    return AccelerateInterpolator((factor))
}

private fun create(zone: PointF?): (Float) -> Float {
    if (zone == null) {
        return { it }
    }
    if (zone.x.isNaN() || zone.y.isNaN()) {
        return { it }
    }

    return { input ->
        val s = input.sign
        translate(valueX = input.absoluteValue, x1 = 0f, x2 = 1f, y1 = zone.x, y2 = zone.y) * s
    }
}
