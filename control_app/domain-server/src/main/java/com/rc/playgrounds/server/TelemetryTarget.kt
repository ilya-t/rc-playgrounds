package com.rc.playgrounds.server

import org.json.JSONObject

data class TelemetryTarget(
    val host: String,
    val port: Int,
)

fun parseTelemetryTarget(json: JSONObject): TelemetryTarget? {
    val port = port(json) ?: return null
    val host = json.optString("client_addr", "")
    if (host.isEmpty()) {
        return null
    }
    return TelemetryTarget(host, port)
}

private fun port(json: JSONObject): Int? {
    if (!json.has("telemetry_port") || json.isNull("telemetry_port")) {
        return null
    }
    val value = json.get("telemetry_port")
    val parsed = when (value) {
        is Number -> {
            val asLong = value.toLong()
            if (value.toDouble() != asLong.toDouble()) {
                return null
            }
            asLong
        }
        is String -> value.toLongOrNull() ?: return null
        else -> return null
    }
    if (parsed !in 1..65535) {
        return null
    }
    return parsed.toInt()
}
