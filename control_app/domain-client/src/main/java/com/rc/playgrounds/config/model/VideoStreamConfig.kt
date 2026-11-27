package com.rc.playgrounds.config.model

import com.rc.playgrounds.common.VideoStream
import com.rc.playgrounds.config.env.applyEnv
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class VideoStreamConfig(
    private val height: String,
    @SerialName("frame_rate")
    private val frameRate: String,
    private val rotation: String,
    private val bitrate: String,
) {
    fun resolve(env: Map<String, String>): VideoStream? {
        val height = height.applyEnv(env).toIntOrNull() ?: return null
        val frameRate = frameRate.applyEnv(env).toIntOrNull() ?: return null
        val rotation = rotation.applyEnv(env).toIntOrNull() ?: return null
        val bitrate = bitrate.applyEnv(env).toIntOrNull() ?: return null
        return VideoStream.valid(height, frameRate, rotation, bitrate)
    }
}
