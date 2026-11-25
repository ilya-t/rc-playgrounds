package com.rc.playgrounds.server

import android.os.BatteryManager
import android.telephony.TelephonyManager
import com.rc.playgrounds.server.controllink.DeviceState
import com.rc.playgrounds.server.controllink.MicroviewProtocol
import com.rc.playgrounds.telemetry.Telemetry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CellularReadingTest {
    @Test
    fun `lte rsrp becomes a 4g percent`() {
        assertEquals(
            Telemetry.Cellular(strength = 0, generation = "4G"),
            cellularSnapshot(TelephonyManager.NETWORK_TYPE_LTE, listOf(-140), emptyList()),
        )
        assertEquals(
            Telemetry.Cellular(strength = 100, generation = "4G"),
            cellularSnapshot(TelephonyManager.NETWORK_TYPE_LTE, listOf(-44, -80), emptyList()),
        )
    }

    @Test
    fun `nr ss rsrp becomes a 5g percent`() {
        assertEquals(
            Telemetry.Cellular(strength = 100, generation = "5G"),
            cellularSnapshot(TelephonyManager.NETWORK_TYPE_NR, listOf(-80), listOf(-31)),
        )
    }

    @Test
    fun `2g and 3g keep generation and omit strength`() {
        assertEquals(
            Telemetry.Cellular(strength = null, generation = "2G"),
            cellularSnapshot(TelephonyManager.NETWORK_TYPE_EDGE, listOf(-80), listOf(-80)),
        )
        assertEquals(
            Telemetry.Cellular(strength = null, generation = "3G"),
            cellularSnapshot(TelephonyManager.NETWORK_TYPE_UMTS, emptyList(), emptyList()),
        )
    }

    @Test
    fun `4g without rsrp omits strength`() {
        assertEquals(
            Telemetry.Cellular(strength = null, generation = "4G"),
            cellularSnapshot(TelephonyManager.NETWORK_TYPE_LTE, emptyList(), listOf(-31)),
        )
    }

    @Test
    fun `unknown network type is omitted`() {
        assertNull(cellularSnapshot(TelephonyManager.NETWORK_TYPE_UNKNOWN, listOf(-80), emptyList()))
    }
}

class BatteryReadingTest {
    @Test
    fun `charging is only the charging status`() {
        assertEquals(
            Telemetry.Battery(charge = 85, charging = true),
            batteryReading(85, 100, BatteryManager.BATTERY_STATUS_CHARGING),
        )
        assertEquals(
            Telemetry.Battery(charge = 100, charging = false),
            batteryReading(100, 100, BatteryManager.BATTERY_STATUS_FULL),
        )
        assertEquals(
            Telemetry.Battery(charge = 40, charging = false),
            batteryReading(40, 100, BatteryManager.BATTERY_STATUS_DISCHARGING),
        )
    }

    @Test
    fun `missing level or scale is omitted`() {
        assertNull(batteryReading(-1, 100, BatteryManager.BATTERY_STATUS_CHARGING))
        assertNull(batteryReading(10, 0, BatteryManager.BATTERY_STATUS_CHARGING))
    }
}

class ServerTelemetryTest {
    @Test
    fun `control gate keeps optional sections`() {
        val status = MicroviewProtocol.Status(
            sequence = 1,
            uptimeMs = 10,
            commandAgeMs = 0,
            deviceState = DeviceState.RECEIVING,
        )
        assertEquals(
            Telemetry(
                control = Telemetry.Control(status = "receiving", error = false),
                cellular = Telemetry.Cellular(strength = 72, generation = "4G"),
                battery = Telemetry.Battery(charge = 85, charging = true),
            ),
            serverTelemetry(
                status,
                Telemetry.Cellular(strength = 72, generation = "4G"),
                Telemetry.Battery(charge = 85, charging = true),
            ),
        )
    }

    @Test
    fun `missing link still sends battery and signal`() {
        assertEquals(
            Telemetry(
                control = Telemetry.Control(status = "no link", error = true),
                cellular = Telemetry.Cellular(strength = null, generation = "2G"),
                battery = Telemetry.Battery(charge = 10, charging = false),
            ),
            serverTelemetry(
                current = null,
                cellular = Telemetry.Cellular(strength = null, generation = "2G"),
                battery = Telemetry.Battery(charge = 10, charging = false),
            ),
        )
    }
}
