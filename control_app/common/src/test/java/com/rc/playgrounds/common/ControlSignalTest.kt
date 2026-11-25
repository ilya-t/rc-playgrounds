package com.rc.playgrounds.common

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ControlSignalTest {
    @Test
    fun `round trip keeps axes and stream fields`() {
        val signal = ControlSignal(
            pitch = 0.5f,
            yaw = -1f,
            steer = 1f,
            long = 0f,
            streamCmd = "udp://10.0.0.2:5000",
            streamCmdHash = "abc",
            clientAddr = "10.0.0.2",
            telemetryPort = 12347,
        )

        val decoded = ControlSignal.fromJson(signal.toJson())

        assertEquals(signal, decoded)
    }

    @Test
    fun `missing axis is zero`() {
        val json = JSONObject()
            .put("pitch", 0.25)
            .put("steer", -0.5)
            .put("long", 0.5)

        val decoded = ControlSignal.fromJson(json)

        assertEquals(
            ControlSignal(
                pitch = 0.25f,
                yaw = 0f,
                steer = -0.5f,
                long = 0.5f,
                streamCmd = "",
                streamCmdHash = "",
            ),
            decoded,
        )
    }

    @Test
    fun `out of range axis is rejected`() {
        val json = JSONObject()
            .put("pitch", 0)
            .put("yaw", 0)
            .put("steer", 1.1)
            .put("long", 0)

        assertNull(ControlSignal.fromJson(json))
    }

    @Test
    fun `non numeric axis is rejected`() {
        val json = JSONObject()
            .put("pitch", "fast")
            .put("yaw", 0)
            .put("steer", 0)
            .put("long", 0)

        assertNull(ControlSignal.fromJson(json))
    }
}
