package com.rc.playgrounds.server

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class TelemetryTargetTest {
    @Test
    fun `client address and telemetry port become the target`() {
        val json = JSONObject()
            .put("client_addr", "10.0.0.2")
            .put("telemetry_port", 12347)

        assertEquals(TelemetryTarget("10.0.0.2", 12347), parseTelemetryTarget(json))
    }

    @Test
    fun `missing address does not use another host`() {
        assertNull(parseTelemetryTarget(JSONObject().put("telemetry_port", 12347)))
    }

    @Test
    fun `invalid port does not produce a target`() {
        assertNull(
            parseTelemetryTarget(
                JSONObject().put("client_addr", "10.0.0.2").put("telemetry_port", 0),
            ),
        )
        assertNull(
            parseTelemetryTarget(
                JSONObject().put("client_addr", "10.0.0.2").put("telemetry_port", 65536),
            ),
        )
    }
}
