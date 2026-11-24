package com.rc.playgrounds.config.stream

import com.rc.playgrounds.config.env.applyEnv
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class StreamConfig(
    @Deprecated("use env profiles instead")
    @SerialName("quality_profiles")
    val qualityProfiles: List<QualityProfile> = emptyList(),
    @SerialName("default_quality_profile_index")
    val defaultQualityProfile: Int? = null,
    @SerialName("remote_cmd")
    private val remoteCmd: String,
    @SerialName("local_cmd")
    private val localCmd: String,
) {
    fun localCmd(env: Map<String, String>): String {
        return localCmd.applyEnv(env)
    }

    fun remoteCmd(env: Map<String, String>): String {
        return remoteCmd.applyEnv(env)
    }
}
