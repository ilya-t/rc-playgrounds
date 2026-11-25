package com.rc.playgrounds.telemetry

import com.rc.playgrounds.common.Clock
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TelemetrySignalTest {
    @Test
    fun `fresh sample is up to date`() {
        val signal = TelemetrySignal(sample(), updatedAtMs = 1_000L)

        assertTrue(signal.isUpToDate(clock(1_250L)))
    }

    @Test
    fun `sample older than ttl is stale but keeps telemetry`() {
        val telemetry = sample()
        val signal = TelemetrySignal(telemetry, updatedAtMs = 1_000L)

        assertFalse(signal.isUpToDate(clock(1_251L)))
        assertEqualsTelemetry(telemetry, signal.telemetry)
    }

    @Test
    fun `empty signal is not up to date`() {
        assertFalse(TelemetrySignal.empty().isUpToDate(clock(1_000L)))
    }

    private fun sample(): Telemetry {
        return Telemetry(Telemetry.Control(status = "receiving", error = false))
    }

    private fun clock(millis: Long): Clock = Clock { millis }

    private fun assertEqualsTelemetry(expected: Telemetry, actual: Telemetry?) {
        org.junit.Assert.assertEquals(expected, actual)
    }
}
