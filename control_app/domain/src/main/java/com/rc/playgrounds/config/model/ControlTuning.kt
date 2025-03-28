package com.rc.playgrounds.config.model

import android.graphics.PointF

data class ControlTuning(
    val pitchFactor: Float?,
    val pitchZone: PointF?,
    val yawFactor: Float?,
    val yawZone: PointF?,
    val steerFactor: Float?,
    val steerZone: PointF?,
    val forwardLongZones: List<MappingZone>,
    val backwardLongZones: List<MappingZone>,
)
