package com.rc.playgrounds.config.model

import kotlinx.serialization.Serializable

@Serializable
data class ControlOffsets(
    val pitch: Float,
    val yaw: Float,
    val steer: Float,
    val long: Float,
)
