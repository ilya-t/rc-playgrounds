package com.rc.playgrounds.control

import android.graphics.PointF
import android.view.animation.BaseInterpolator
import com.rc.playgrounds.config.ActiveConfigProvider
import com.rc.playgrounds.config.model.MappingZone
import com.rc.playgrounds.control.steering.SteerValue
import com.rc.playgrounds.control.tuning.ExponentInterpolator
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
        tuning.asInterpolation()
    }
}

class ControlInterpolation(
    private val pitch: BaseInterpolator?,
    private val pitchTranslator: (Float) -> Float,
    private val yaw: BaseInterpolator?,
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
        interpolator: BaseInterpolator?,
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

private fun ControlTuning.asInterpolation() = ControlInterpolation(
    pitch = createInterpolator(pitchFactor),
    pitchTranslator = createZoneTranslation(pitchZone),
    yaw = createInterpolator(yawFactor),
    yawTranslator = createZoneTranslation(yawZone),
    steerTranslator = { steer: Float, rawTrigger: Float ->
        val trigger = rawTrigger.absoluteValue
        steerLimitAtTrigger
            .find { trigger >= it.src.x && trigger <= it.src.y }
            ?.dst
            ?.let { limits ->
                val maxSteer = translate(trigger, 0f, 1f, limits.x, limits.y)
                translate(steer.absoluteValue, 0f, 1f, 0f, maxSteer) * steer.sign
            }

            ?: steerZone?.let {
                translate(steer.absoluteValue, 0f, 1f, it.x, it.y) * steer.sign
            }

            ?: steer
    },
    longTranslator = createLongTranslationByZones(
        negative = backwardLongZones.ifEmpty { forwardLongZones },
        positive = forwardLongZones,
        interpolator = createInterpolator(longFactor),
    ),
)

private fun createLongTranslationByZones(negative: List<MappingZone>,
                                         positive: List<MappingZone>,
                                         interpolator: BaseInterpolator?): (Float) -> Float {
    return { input: Float ->
        val zones = if (input >= 0) {
            positive
        } else {
            negative
        }
        val absInterpolatedInput = interpolator?.getInterpolation(input.absoluteValue)
            ?: input.absoluteValue

        val targetZone: MappingZone? = zones
            .find { absInterpolatedInput >= it.src.x && absInterpolatedInput <= it.src.y }
            ?: zones.lastOrNull()

        targetZone
            ?.let {
                translate(absInterpolatedInput, it.src.x, it.src.y, it.dst.x, it.dst.y)
            }
            ?.withSign(input)
            ?: absInterpolatedInput.withSign(input)
    }
}


private fun createInterpolator(factor: Float?): BaseInterpolator? {
    if (factor == null) {
        return null
    }

    if (factor.isNaN()) {
        return null
    }

    return ExponentInterpolator(factor)
}

private fun createZoneTranslation(zone: PointF?): (Float) -> Float {
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
