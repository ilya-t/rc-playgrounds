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
    fun `cellular and battery round trip`() {
        val original = Telemetry(
            control = Telemetry.Control(status = "receiving", error = false),
            cellular = Telemetry.Cellular(strength = 72, generation = "4G"),
            battery = Telemetry.Battery(charge = 85, charging = true),
        )

        assertEquals(original, Telemetry.fromJson(original.toJson()))
    }

    @Test
    fun `cellular generation without strength round trips`() {
        val original = Telemetry(
            control = Telemetry.Control(status = "receiving", error = false),
            cellular = Telemetry.Cellular(strength = null, generation = "3G"),
        )

        val decoded = Telemetry.fromJson(original.toJson())
        assertEquals(original, decoded)
        assertNull(decoded?.cellular?.strength)
    }

    @Test
    fun `broken optional sections are dropped and control remains`() {
        val decoded = Telemetry.fromJson(
            """
            {
              "control": {"status":"failsafe","error":true},
              "cellular": {"generation":"LTE","strength":72},
              "battery": {"charge":120,"charging":true}
            }
            """.trimIndent(),
        )

        assertEquals(
            Telemetry(control = Telemetry.Control(status = "failsafe", error = true)),
            decoded,
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

class SignalStrengthPercentTest {
    @Test
    fun `lte rsrp maps linearly and clamps`() {
        assertEquals(0, signalStrengthPercent(-140, LTE_RSRP_MIN_DBM, LTE_RSRP_MAX_DBM))
        assertEquals(100, signalStrengthPercent(-44, LTE_RSRP_MIN_DBM, LTE_RSRP_MAX_DBM))
        assertEquals(50, signalStrengthPercent(-92, LTE_RSRP_MIN_DBM, LTE_RSRP_MAX_DBM))
        assertEquals(0, signalStrengthPercent(-156, LTE_RSRP_MIN_DBM, LTE_RSRP_MAX_DBM))
        assertEquals(100, signalStrengthPercent(-31, LTE_RSRP_MIN_DBM, LTE_RSRP_MAX_DBM))
    }

    @Test
    fun `nr rsrp maps linearly and clamps`() {
        assertEquals(0, signalStrengthPercent(-156, NR_RSRP_MIN_DBM, NR_RSRP_MAX_DBM))
        assertEquals(100, signalStrengthPercent(-31, NR_RSRP_MIN_DBM, NR_RSRP_MAX_DBM))
        assertEquals(0, signalStrengthPercent(-180, NR_RSRP_MIN_DBM, NR_RSRP_MAX_DBM))
        assertEquals(100, signalStrengthPercent(-20, NR_RSRP_MIN_DBM, NR_RSRP_MAX_DBM))
    }
}
