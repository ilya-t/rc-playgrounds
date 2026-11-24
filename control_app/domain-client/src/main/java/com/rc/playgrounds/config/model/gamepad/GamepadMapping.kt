package com.rc.playgrounds.config.model.gamepad

import com.rc.playgrounds.control.gamepad.GamepadEvent
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GamepadMapping(
    @SerialName("steer")
    val steer: GamepadAxisMapping,
    @SerialName("long")
    val long: GamepadAxisMapping,
    @SerialName("yaw")
    val yaw: GamepadAxisMapping,
    @SerialName("pitch")
    val pitch: GamepadAxisMapping,
) {
    companion object {
        val DEFAULT = GamepadMapping(
            steer = GamepadAxisMapping(axis = GamepadAxis.LEFT_STICK_X),
            long = GamepadAxisMapping(axis = GamepadAxis.TRIGGERS),
            yaw = GamepadAxisMapping(axis = GamepadAxis.RIGHT_STICK_X),
            pitch = GamepadAxisMapping(axis = GamepadAxis.RIGHT_STICK_Y),
        )
    }
}

@Serializable
data class GamepadAxisMapping(
    @SerialName("axis")
    val axis: GamepadAxis,
) {
    fun resolveAxis(event: GamepadEvent): Float {
        return when (axis) {
            GamepadAxis.TRIGGERS -> event.longTrigger()
            GamepadAxis.LEFT_STICK_X -> event.leftStickX
            GamepadAxis.LEFT_STICK_Y -> event.leftStickY
            GamepadAxis.RIGHT_STICK_X -> event.rightStickX
            GamepadAxis.RIGHT_STICK_Y -> event.rightStickY
        }
    }

    private fun GamepadEvent.longTrigger(): Float {
        val breakTrigger = leftTrigger
        return if (breakTrigger > rightTrigger) {
            breakTrigger
        } else {
            -rightTrigger
        }
    }

}


