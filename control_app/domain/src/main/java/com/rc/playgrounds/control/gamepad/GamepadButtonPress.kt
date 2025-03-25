package com.rc.playgrounds.control.gamepad

sealed interface GamepadButtonPress {
    data object DpadUp : GamepadButtonPress
    data object DpadDown : GamepadButtonPress
}
