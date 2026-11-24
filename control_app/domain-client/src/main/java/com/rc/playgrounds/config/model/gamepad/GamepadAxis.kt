package com.rc.playgrounds.config.model.gamepad

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class GamepadAxis {
    @SerialName("left_right_triggers")
    TRIGGERS,

    @SerialName("left_stick_X")
    LEFT_STICK_X,

    @SerialName("left_stick_Y")
    LEFT_STICK_Y,

    @SerialName("right_stick_X")
    RIGHT_STICK_X,

    @SerialName("right_stick_Y")
    RIGHT_STICK_Y,
}