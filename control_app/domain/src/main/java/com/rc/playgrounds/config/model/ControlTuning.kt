package com.rc.playgrounds.config.model

import android.graphics.PointF

data class ControlTuning(
    val pitchFactor: Float?,
    val pitchZone: PointF?,
    val yawFactor: Float?,
    val yawZone: PointF?,
    val steerFactor: Float?,
    val steerZone: PointF?,
    val longFactor: Float?,
    val longZones: List<MappingZone>,
    val longZonesNegative: List<MappingZone>,
)
