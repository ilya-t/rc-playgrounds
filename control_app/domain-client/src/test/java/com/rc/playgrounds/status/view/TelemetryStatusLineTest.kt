package com.rc.playgrounds.status.view

import com.rc.playgrounds.telemetry.Telemetry
import org.junit.Assert.assertEquals
import org.junit.Test

class TelemetryStatusLineTest {
    @Test
    fun `cellular line includes strength when present`() {
        assertEquals("- signal: 72% 4G", cellularLine(Telemetry.Cellular(strength = 72, generation = "4G")))
        assertEquals("- signal: 3G", cellularLine(Telemetry.Cellular(strength = null, generation = "3G")))
    }

    @Test
    fun `battery line adds charging only while charging`() {
        assertEquals("- server.battery: 85% charging", batteryLine(Telemetry.Battery(charge = 85, charging = true)))
        assertEquals("- server.battery: 85%", batteryLine(Telemetry.Battery(charge = 85, charging = false)))
    }
}
