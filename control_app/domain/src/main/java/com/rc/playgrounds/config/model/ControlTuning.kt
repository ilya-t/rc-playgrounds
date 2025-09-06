package com.rc.playgrounds.config.model

import android.graphics.PointF
import com.rc.playgrounds.config.serial.Zones
import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
data class ControlTuning(
    @SerialName("pitch_factor")
    val pitchFactor: Float? = null,
    @SerialName("pitch_zone")
    internal val rawPitchZone: String? = null,
    @SerialName("yaw_factor")
    val yawFactor: Float? = null,
    @Contextual
    @SerialName("yaw_zone")
    val rawYawZone: String? = null,
    @SerialName("steer_zone")
    val rawSteerZone: String? = null,

    @SerialName("steer_mode")
    val steerMode: String? = null,

    @SerialName("steer_exponent_factor")
    val steerExponentFactor: Float? = null,
    /**
     * Key is your trigger position for long. Value is steering limit at that position
     */
    @SerialName("steer_limit_at_trigger")
    val rawSteerLimitAtTrigger: Map<String, String> = emptyMap(),

    /**
     * Key is your right trigger position. Value is 'long' value that will be sent to the car.
     */
    @SerialName("forward_long_zones")
    val rawForwardLongZones: Map<String, String> = emptyMap(),

    /**
     * Key is your left trigger position. Value is 'long' value that will be sent to the car.
     */
    @SerialName("backward_long_zones")
    val rawBackwardLongZones: Map<String, String> = emptyMap(),

    @SerialName("wheel")
    val wheel: WheelConfig? = null,
) {
    @Transient
    val pitchZone: PointF? = Zones.parseZone(rawPitchZone)
    @Transient
    val yawZone: PointF? = Zones.parseZone(rawYawZone)
    @Transient
    val steerZone: PointF? = Zones.parseZone(rawSteerZone)
    @Transient
    val forwardLongZones: List<MappingZone> = Zones.parseZones(rawForwardLongZones)
    @Transient
    val backwardLongZones: List<MappingZone> = Zones.parseZones(rawBackwardLongZones)

    @Transient
    val steerLimitAtTrigger: List<MappingZone> = Zones.parseZones(rawSteerLimitAtTrigger)
}
