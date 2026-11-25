package com.rc.playgrounds.config.model

import com.rc.playgrounds.config.env.applyEnv
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MobileClient(
    private val address: String,
    @SerialName("telemetry_port")
    private val telemetryPort: String = DEFAULT_TELEMETRY_PORT.toString(),
) {
    fun address(env: Map<String, String>): String {
        return address.applyEnv(env)
    }

    fun telemetryPort(env: Map<String, String>): Int {
        val parsed = telemetryPort.applyEnv(env).toIntOrNull()
        return if (parsed != null && parsed in 1..65535) parsed else DEFAULT_TELEMETRY_PORT
    }

    companion object {
        const val DEFAULT_TELEMETRY_PORT = 12347
    }
}
