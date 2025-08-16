package com.rc.playgrounds.control

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
        val STILL = RcEvent(
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