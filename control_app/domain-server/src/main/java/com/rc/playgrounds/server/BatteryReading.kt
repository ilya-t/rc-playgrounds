package com.rc.playgrounds.server

import android.os.BatteryManager
import com.rc.playgrounds.telemetry.Telemetry

internal fun batteryReading(level: Int, scale: Int, status: Int): Telemetry.Battery? {
    if (level < 0 || scale <= 0) {
        return null
    }
    val charge = ((level * 100L + scale / 2) / scale).toInt().coerceIn(0, 100)
    val charging = status == BatteryManager.BATTERY_STATUS_CHARGING
    return Telemetry.Battery(charge = charge, charging = charging)
}
