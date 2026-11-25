package com.rc.playgrounds.config.model

import com.rc.playgrounds.config.env.applyEnv
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MobileClient(
    private val address: String,
    @SerialName("telemetry_port")
    private val telemetryPort: String,
) {
    fun address(env: Map<String, String>): String {
        return address.applyEnv(env)
    }

    fun telemetryPort(env: Map<String, String>): Int? {
        val parsed = telemetryPort.applyEnv(env).toIntOrNull()
        return parsed?.takeIf { it in 1..65535 }
    }
}
