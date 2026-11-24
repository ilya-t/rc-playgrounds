package com.rc.playgrounds.config.model

import com.rc.playgrounds.config.env.applyEnv
import kotlinx.serialization.Serializable

@Serializable
data class NetworkTarget(
    private val address: String,
    private val port: String,
) {
    fun address(env: Map<String, String>): String {
        return address.applyEnv(env)
    }

    fun port(env: Map<String, String>): Int {
        return port.applyEnv(env).toInt()
    }
}
