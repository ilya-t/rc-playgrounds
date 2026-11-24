package com.rc.playgrounds.control

import com.rc.playgrounds.control.steering.SteerValue

data class RcEvent(
    val pitch: Float,
    val yaw: Float,
    val steer: Float,
    val long: Float,
    val rawPitch: Float,
    val rawYaw: Float,
    val rawSteer: Float,
    val rawLong: Float
) {
    companion object {
        fun create(
            pitch: Float,
            yaw: Float,
            steer: SteerValue,
            long: Float,
            rawPitch: Float,
            rawYaw: Float,
            rawSteer: Float,
            rawLong: Float,
        ) = RcEvent(
            pitch = pitch.coerceIn(-1f, 1f),
            yaw = yaw.coerceIn(-1f, 1f),
            steer = steer.coerceIn(-1f, 1f),
            long = long.coerceIn(-1f, 1f),
            rawPitch = rawPitch,
            rawYaw = rawYaw,
            rawSteer = rawSteer,
            rawLong = rawLong,
        )

        val STILL: RcEvent = create(
            pitch = 0f,
            yaw = 0f,
            steer = 0f,
            long = 0f,
            rawPitch = 0f,
            rawYaw = 0f,
            rawSteer = 0f,
            rawLong = 0f
        )
    }
}