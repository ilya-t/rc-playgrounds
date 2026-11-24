package com.rc.playgrounds.common

import org.json.JSONObject

data class ControlSignal(
    val pitch: Float,
    val yaw: Float,
    val steer: Float,
    val long: Float,
    val streamCmd: String,
    val streamCmdHash: String,
) {
    fun toJson(): JSONObject {
        return JSONObject()
            .put("pitch", pitch)
            .put("yaw", yaw)
            .put("steer", steer)
            .put("long", long)
            .put("stream_cmd", streamCmd)
            .put("stream_cmd_hash", streamCmdHash)
    }

    companion object {
        fun fromJson(json: JSONObject): ControlSignal? {
            val yaw = axis(json, "yaw") ?: return null
            val pitch = axis(json, "pitch") ?: return null
            val steer = axis(json, "steer") ?: return null
            val longValue = axis(json, "long") ?: return null
            if (!inRange(yaw) || !inRange(pitch) || !inRange(steer) || !inRange(longValue)) {
                return null
            }
            return ControlSignal(
                pitch = pitch.toFloat(),
                yaw = yaw.toFloat(),
                steer = steer.toFloat(),
                long = longValue.toFloat(),
                streamCmd = json.optString("stream_cmd", ""),
                streamCmdHash = json.optString("stream_cmd_hash", ""),
            )
        }

        private fun axis(json: JSONObject, name: String): Double? {
            if (!json.has(name)) {
                return 0.0
            }
            if (json.isNull(name)) {
                return null
            }
            val value = json.get(name)
            if (value !is Number) {
                return null
            }
            return value.toDouble()
        }

        private fun inRange(value: Double): Boolean {
            return value in -1.0..1.0
        }
    }
}
