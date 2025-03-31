package com.rc.playgrounds.config.stream

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class StreamConfig(
    @SerialName("quality_profiles")
    val qualityProfiles: List<QualityProfile>,
    @SerialName("remote_cmd")
    val remoteCmd: String,
    @SerialName("local_cmd")
    val localCmd: String,
)
