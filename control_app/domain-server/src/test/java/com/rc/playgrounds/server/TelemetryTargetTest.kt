package com.rc.playgrounds.server

import com.rc.playgrounds.common.ControlSignal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class TelemetryTargetTest {
    @Test
    fun `target copies address and port from the control signal`() {
        assertEquals(
            TelemetryTarget("10.0.0.2", 12347),
            parseTelemetryTarget(signal(clientAddr = "10.0.0.2", telemetryPort = 12347)),
        )
    }

    @Test
    fun `missing address or port does not invent a target`() {
        assertNull(parseTelemetryTarget(signal(telemetryPort = 12347)))
        assertNull(parseTelemetryTarget(signal(clientAddr = "10.0.0.2", telemetryPort = null)))
    }

    private fun signal(
        clientAddr: String = "",
        telemetryPort: Int? = null,
    ): ControlSignal {
        return ControlSignal(
            pitch = 0f,
            yaw = 0f,
            steer = 0f,
            long = 0f,
            streamCmd = "",
            streamCmdHash = "",
            clientAddr = clientAddr,
            telemetryPort = telemetryPort,
        )
    }
}
