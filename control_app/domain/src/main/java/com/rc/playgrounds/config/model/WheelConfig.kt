package com.rc.playgrounds.config.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class WheelConfig(
    @SerialName("max_angle_deg") val maxAngleDeg: Float? = null,
    @SerialName("max_turn_rate_deg_per_sec") val maxTurnRateDegPerSec: Float? = null,
    @SerialName("center_return_rate_deg_per_sec") val centerReturnRateDegPerSec: Float? = null,
    @SerialName("deadzone") val deadzone: Float? = null,
    @SerialName("curve_blend") val curveBlend: Float? = null,
    @SerialName("ema_cutoff_hz") val emaCutoffHz: Float? = null,
    @SerialName("center_stick_threshold") val centerStickThreshold: Float? = null,
    @SerialName("damping") val damping: Float? = null,
)
