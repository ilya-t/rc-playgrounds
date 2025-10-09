package com.rc.playgrounds.control

import android.graphics.PointF
import com.rc.playgrounds.config.model.MappingZone
import com.rc.playgrounds.config.model.WheelConfig

data class ControlTuning(
    val steerMode: String?,
    val pitchFactor: Float?,
    val yawFactor: Float?,
    val pitchZone: PointF?,
    val yawZone: PointF?,
    val steerZone: PointF?,
    val steerExponentFactor: Float?,
    val forwardLongZones: List<MappingZone>,
    val backwardLongZones: List<MappingZone>,
    val steerLimitAtTrigger: List<MappingZone>,
    val wheel: WheelConfig?,
)
