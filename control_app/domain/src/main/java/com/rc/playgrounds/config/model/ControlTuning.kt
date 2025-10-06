package com.rc.playgrounds.config.model

import android.graphics.PointF
import com.rc.playgrounds.config.env.applyEnv
import com.rc.playgrounds.config.serial.Zones
import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
data class ControlTuning(
    @SerialName("name")
    val name: String? = "initial",
    @SerialName("pitch_factor")
    val pitchFactor: String? = null,
    @SerialName("pitch_zone")
    internal val rawPitchZone: String? = null,
    @SerialName("yaw_factor")
    val yawFactor: String? = null,
    @Contextual
    @SerialName("yaw_zone")
    val rawYawZone: String? = null,
    @SerialName("steer_zone")
    val rawSteerZone: String? = null,

    @SerialName("steer_mode")
    val steerMode: String? = null,

    @SerialName("steer_exponent_factor")
    val steerExponentFactor: String? = null,
    /**
     * Key/value pairs in format "key:value;key1:value1;".
     * Key is your trigger position for long. Value is steering limit at that position
     */
    @SerialName("steer_limit_at_trigger")
    val rawSteerLimitAtTrigger: String? = null,

    /**
     * Key/value pairs in format "key:value;key1:value1;".
     * Key is your right trigger position. Value is 'long' value that will be sent to the car.
     */
    @SerialName("forward_long_zones")
    val rawForwardLongZones: String? = null,

    /**
     * Key/value pairs in format "key:value;key1:value1;".
     * Key is your left trigger position. Value is 'long' value that will be sent to the car.
     */
    @SerialName("backward_long_zones")
    val rawBackwardLongZones: String? = null,

    @SerialName("wheel")
    val wheel: WheelConfig? = null,
) {
    @Transient
    val forwardLongZones: List<MappingZone> = Zones.parseZones(rawForwardLongZones)
    @Transient
    val backwardLongZones: List<MappingZone> = Zones.parseZones(rawBackwardLongZones)

    @Transient
    val steerLimitAtTrigger: List<MappingZone> = Zones.parseZones(rawSteerLimitAtTrigger)

    fun pitchFactor(env: Map<String, String>): Float? {
        return pitchFactor?.applyEnv(env)?.toFloat()
    }

    fun yawFactor(env: Map<String, String>): Float? {
        return yawFactor?.applyEnv(env)?.toFloat()
    }

    fun pitchZone(env: Map<String, String>): PointF?  {
        return Zones.parseZone(rawPitchZone?.applyEnv(env))
    }

    fun yawZone(env: Map<String, String>): PointF?  {
        return Zones.parseZone(rawYawZone?.applyEnv(env))
    }

    fun steerZone(env: Map<String, String>): PointF?  {
        return Zones.parseZone(rawSteerZone?.applyEnv(env))
    }

    fun steerExponentFactor(env: Map<String, String>): Float? {
        return steerExponentFactor?.applyEnv(env)?.toFloat()
    }

}
