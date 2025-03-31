package com.rc.playgrounds.config.model

import kotlinx.serialization.Serializable

@Serializable
data class NetworkTarget(
    val address: String,
    val port: Int,
)