package com.rc.playgrounds.control.gamepad

sealed interface GamepadButtonPress {
    data object Up : GamepadButtonPress
    data object Down : GamepadButtonPress
    data object Left : GamepadButtonPress
    data object Right : GamepadButtonPress
    data object A : GamepadButtonPress
    data object B : GamepadButtonPress
    data object START : GamepadButtonPress
    data object SELECT : GamepadButtonPress
    data object LeftBumper : GamepadButtonPress
    data object RightBumper : GamepadButtonPress
}
