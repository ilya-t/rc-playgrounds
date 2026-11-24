package com.rc.playgrounds.control.gamepad

data class GamepadEvent(
    val leftTrigger: Float,
    val rightTrigger: Float,
    val leftStickX: Float,
    val leftStickY: Float,
    val rightStickX: Float,
    val rightStickY: Float,
) {
    companion object {
        val INITIAL = GamepadEvent(
            leftTrigger = 0f,
            rightTrigger = 0f,
            leftStickX = 0f,
            leftStickY = 0f,
            rightStickX = 0f,
            rightStickY = 0f,
        )
    }
}
