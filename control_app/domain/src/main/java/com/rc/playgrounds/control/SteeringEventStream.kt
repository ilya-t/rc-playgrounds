package com.rc.playgrounds.control

import android.graphics.PointF
import android.view.animation.AccelerateInterpolator
import com.rc.playgrounds.config.ActiveConfigProvider
import com.rc.playgrounds.config.model.ControlOffsets
import com.rc.playgrounds.config.model.MappingZone
import com.rc.playgrounds.control.gamepad.GamepadEvent
import com.rc.playgrounds.control.gamepad.GamepadEventStream
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue
import kotlin.math.sign
import kotlin.math.withSign

class SteeringEventStream(
    scope: CoroutineScope,
    private val activeConfigProvider: ActiveConfigProvider,
    private val gamepadEventStream: GamepadEventStream,
) {
    private val statelessEvents: Flow<SteeringEvent> = combine(
        activeConfigProvider.configFlow.map { it.controlOffsets },
        activeConfigProvider.configFlow.map { it.controlTuning.asInterpolation() },
        gamepadEventStream.events,
    ) { offsets: ControlOffsets,
        interpolation: ControlInterpolation,
        event: GamepadEvent ->

        val rawPitch = -event.rightStickY
        val rawYaw = event.rightStickX
        val rawSteer = -event.leftStickX
        val breakTrigger = event.leftTrigger
        val rightTrigger = event.rightTrigger
        val longTrigger = if (breakTrigger > rightTrigger) {
            event.leftTrigger
        } else {
            -rightTrigger
        }
        val rawLong = -longTrigger

        val steeringEvent = SteeringEvent(
            pitch = interpolation.fixPitch(rawPitch) + offsets.pitch,
            yaw = interpolation.fixYaw(rawYaw) + offsets.yaw,
            steer = interpolation.fixSteer(rawSteer) + offsets.steer,
            long = if (rawLong.absoluteValue > 0.0001f) {
                // When trigger not touched then its safer to prevent long
                // pulse at all rather than send one with offset.
                interpolation.fixLong(rawLong) + (if (rawLong < 0) -offsets.long else offsets.long)
            } else {
                0f
            },
            rawPitch = rawPitch,
            rawYaw = rawYaw,
            rawSteer = rawSteer,
            rawLong = rawLong,
        )
        steeringEvent
    }

    private val _events = MutableStateFlow(SteeringEvent.STILL)
    val events: Flow<SteeringEvent> = _events

    init {
        scope.launch {
            statelessEvents.collect {
                _events.value = it
            }
        }
    }
}

private fun com.rc.playgrounds.config.model.ControlTuning.asInterpolation() = ControlInterpolation(
    pitch = create(pitchFactor),
    pitchTranslator = create(pitchZone),
    yaw = create(yawFactor),
    yawTranslator = create(yawZone),
    steer = create(steerFactor),
    steerTranslator = create(steerZone),
    longTranslator = if (forwardLongZones.isNotEmpty()) {
        val zonesNegative = backwardLongZones.ifEmpty { forwardLongZones }
        create(negative = zonesNegative, positive = forwardLongZones)
    } else {
        create(PointF(0f, 1f))
    },
)

fun create(negative: List<MappingZone>, positive: List<MappingZone>): (Float) -> Float {
    return { input ->
        val zones = if (input >= 0) {
            positive
        } else {
            negative
        }
        val absInput = input.absoluteValue
        zones
            .find { absInput >= it.src.x && absInput <= it.src.y }
            ?.let {
                translate(absInput, it.src.x, it.src.y, it.dst.x, it.dst.y)
            }
            ?.withSign(input)
            ?: input
    }
}

private class ControlInterpolation(
    private val pitch: AccelerateInterpolator?,
    private val pitchTranslator: (Float) -> Float,
    private val yaw: AccelerateInterpolator?,
    private val yawTranslator: (Float) -> Float,
    private val steer: AccelerateInterpolator?,
    private val steerTranslator: (Float) -> Float,
    private val longTranslator: (Float) -> Float,
) {
    fun fixPitch(value: Float): Float {
        return fix(pitch, pitchTranslator, value)
    }

    fun fixYaw(value: Float): Float {
        return fix(yaw, yawTranslator, value)
    }

    fun fixSteer(value: Float): Float {
        return fix(steer, steerTranslator, value)
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

fun translate(valueX: Float, x1: Float, x2: Float, y1: Float, y2: Float): Float {
    val raw = y1 + (valueX - x1) * (y2 - y1) / (x2 - x1)
    return raw.coerceIn(y1, y2)
}
