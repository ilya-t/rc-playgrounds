package com.rc.playgrounds.config.model

import com.rc.playgrounds.config.env.applyEnv
import kotlinx.serialization.Serializable

@Serializable
data class NetworkTarget(
    private val address: String,
    val port: Int,
) {
    fun address(env: Map<String, String>): String {
        return address.applyEnv(env)
    }
}
