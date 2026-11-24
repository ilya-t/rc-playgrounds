package com.rc.playgrounds.presentation.quickconfig

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class EnvironmentOverrides(
    @SerialName("name")
    val name: String,
    @SerialName("last_active_index")
    val lastActiveIndex: Int? = null,
    @SerialName("override_profiles")
    val profiles: List<OverrideProfile>,
)

@Serializable
class OverrideProfile(
    @SerialName("name")
    val name: String,
    @SerialName("environment_variables")
    val env: Map<String, String> = emptyMap(),
)
