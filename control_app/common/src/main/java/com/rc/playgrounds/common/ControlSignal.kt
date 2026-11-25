package com.rc.playgrounds.common

import org.json.JSONObject

data class ControlSignal(
    val pitch: Float,
    val yaw: Float,
    val steer: Float,
    val long: Float,
    val streamCmd: String,
    val streamCmdHash: String,
    val clientAddr: String = "",
    val telemetryPort: Int = DEFAULT_TELEMETRY_PORT,
) {
    fun toJson(): JSONObject {
        return JSONObject()
            .put("pitch", pitch)
            .put("yaw", yaw)
            .put("steer", steer)
            .put("long", long)
            .put("stream_cmd", streamCmd)
            .put("stream_cmd_hash", streamCmdHash)
            .put("client_addr", clientAddr)
            .put("telemetry_port", telemetryPort)
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
                clientAddr = json.optString("client_addr", ""),
                telemetryPort = telemetryPort(json),
            )
        }

        private fun telemetryPort(json: JSONObject): Int {
            if (!json.has("telemetry_port") || json.isNull("telemetry_port")) {
                return DEFAULT_TELEMETRY_PORT
            }
            val value = json.get("telemetry_port")
            val parsed = when (value) {
                is Number -> {
                    val asLong = value.toLong()
                    if (value.toDouble() != asLong.toDouble()) {
                        return DEFAULT_TELEMETRY_PORT
                    }
                    asLong
                }
                is String -> value.toLongOrNull() ?: return DEFAULT_TELEMETRY_PORT
                else -> return DEFAULT_TELEMETRY_PORT
            }
            if (parsed !in 1..65535) {
                return DEFAULT_TELEMETRY_PORT
            }
            return parsed.toInt()
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

        const val DEFAULT_TELEMETRY_PORT = 12347
    }
}
