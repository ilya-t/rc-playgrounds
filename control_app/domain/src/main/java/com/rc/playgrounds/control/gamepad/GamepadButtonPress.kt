package com.rc.playgrounds.control.gamepad

sealed interface GamepadButtonPress {
    data object DpadUp : GamepadButtonPress
    data object DpadDown : GamepadButtonPress
    data object B : GamepadButtonPress
    data object START : GamepadButtonPress
    data object SELECT : GamepadButtonPress
}
