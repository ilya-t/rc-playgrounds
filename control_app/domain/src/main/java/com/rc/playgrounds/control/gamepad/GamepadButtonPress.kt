package com.rc.playgrounds.control.gamepad

sealed interface GamepadButtonPress {
    data object Up : GamepadButtonPress
    data object Down : GamepadButtonPress
    data object B : GamepadButtonPress
    data object START : GamepadButtonPress
    data object SELECT : GamepadButtonPress
}
