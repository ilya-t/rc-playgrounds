package com.rc.playgrounds.control

import com.rc.playgrounds.config.ActiveConfigProvider
import com.rc.playgrounds.config.model.ControlOffsets
import com.rc.playgrounds.control.gamepad.GamepadEvent
import com.rc.playgrounds.control.gamepad.GamepadEventStream
import com.rc.playgrounds.control.lock.ControlLock
import com.rc.playgrounds.control.steering.SteerProvider
import com.rc.playgrounds.control.steering.SteerValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue

class RcEventStream(
    scope: CoroutineScope,
    controlInterpolationProvider: ControlInterpolationProvider,
    private val activeConfigProvider: ActiveConfigProvider,
    private val gamepadEventStream: GamepadEventStream,
    private val steerProvider: SteerProvider,
    private val controlLock: ControlLock,
) {

    private val statelessEvents: Flow<RcEvent> = combine(
        controlLock.locked,
        activeConfigProvider.configFlow.map { it.controlOffsets },
        controlInterpolationProvider.interpolation,
        gamepadEventStream.events,
        steerProvider.steer,
    ) { controlsLocked: Boolean,
        offsets: ControlOffsets,
        interpolation: ControlInterpolation,
        event: GamepadEvent,
        steer: SteerValue ->
        if (controlsLocked) {
            return@combine RcEvent.STILL
        }

        val rawPitch = -event.rightStickY
        val rawYaw = event.rightStickX
        val rawSteer = event.leftStickX
        val longTrigger = event.longTrigger
        val rawLong = -longTrigger

        val rcEvent = RcEvent.create(
            pitch = interpolation.fixPitch(rawPitch) + offsets.pitch,
            yaw = interpolation.fixYaw(rawYaw) + offsets.yaw,
            steer = steer,
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
        rcEvent
    }

    private val _events = MutableStateFlow(RcEvent.STILL)
    val events: Flow<RcEvent> = _events

    init {
        scope.launch {
            statelessEvents.collect {
                _events.value = it
            }
        }
    }
}

internal val GamepadEvent.longTrigger: Float
    get() {
        val breakTrigger = leftTrigger
        return if (breakTrigger > rightTrigger) {
            breakTrigger
        } else {
            -rightTrigger
        }
    }

fun translate(valueX: Float, x1: Float, x2: Float, y1: Float, y2: Float): Float {
    val raw = y1 + (valueX - x1) * (y2 - y1) / (x2 - x1)
    return if (y1 > y2) {
        raw.coerceIn(y2, y1)
    } else {
        raw.coerceIn(y1, y2)
    }
}
