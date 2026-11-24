package com.rc.playgrounds.telemetry

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class TelemetryTest {
    @Test
    fun `json round trip keeps control status`() {
        val original = Telemetry(
            control = Telemetry.Control(status = "failsafe", error = true),
        )

        assertEquals(original, Telemetry.fromJson(original.toJson()))
    }

    @Test
    fun `receiving status is not an error`() {
        assertEquals(
            Telemetry(Telemetry.Control(status = "receiving", error = false)),
            Telemetry.fromJson("""{"control":{"status":"receiving","error":false}}"""),
        )
    }

    @Test
    fun `broken json is ignored`() {
        assertNull(Telemetry.fromJson("not-json"))
        assertNull(Telemetry.fromJson("""{"status":"failsafe","error":true}"""))
        assertNull(Telemetry.fromJson("""{"control":{"status":"failsafe"}}"""))
        assertNull(Telemetry.fromJson("""{"control":{"error":true}}"""))
        assertNull(Telemetry.fromJson("""{"control":{"status":"","error":true}}"""))
    }
}
