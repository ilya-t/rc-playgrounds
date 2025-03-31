package com.rc.playgrounds.config

import com.rc.playgrounds.config.model.MappingZone
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ConfigTest {
    private val errorRaiser = { it: Throwable -> throw AssertionError(it) }
    @Test
    fun smoke() {
        val config = Config(DEFAULT_CONFIG, errorRaiser)
        Assert.assertNotNull(config.controlTuning)
        Assert.assertNotNull(config.controlTuning.pitchFactor)
        Assert.assertNotNull(config.controlTuning.pitchZone)
        Assert.assertNotNull(config.controlTuning.yawFactor)
        Assert.assertNotNull(config.controlTuning.yawZone)
        Assert.assertNotNull(config.controlTuning.steerFactor)
        Assert.assertNotNull(config.controlTuning.steerZone)
        Assert.assertTrue(config.controlTuning.forwardLongZones.isNotEmpty())
        Assert.assertTrue(config.controlTuning.backwardLongZones.isNotEmpty())

        Assert.assertNotNull(config.stream.localCmd)
        Assert.assertNotNull(config.stream.remoteCmd)
        Assert.assertTrue(config.stream.qualityProfiles.isNotEmpty())
        Assert.assertNotNull(config.controlServer)
        Assert.assertNotNull(config.controlOffsets)
        Assert.assertNotNull(config.controlTuning)
    }

    @Test
    fun testRemoteStreamCmd_ValidJson() {
        val json = "{\"stream\": {\"remote_cmd\": \"start\"}}"
        val config = Config(json)
        Assert.assertEquals("start", config.stream.remoteCmd)
    }

    @Test
    fun testRemoteStreamCmd_InvalidJson() {
        val json = "{}"
        val config = Config(json)
        Assert.assertEquals("", config.stream.remoteCmd)
    }

    @Test
    fun `long zones parsing`() {
        val json = """{
            "stream": {
            },
            "control_server": {
                "address": "192.168.0.1", 
                "port": 8080
            },
            "stream_target": {
                "address": "192.168.0.2", 
                "port": 8181
            },
            "control_offsets": {
                "pitch": 0.0,
                "yaw": 0.0,
                "steer": 0.0,
                "long": 0.0
            },
            "control_tuning": {
                "forward_long_zones": {
                    "0": "0.01",
                    "0.3": "0.21",
                    "0.7": "0.4",
                    "0.9": "0.5",
                    "1": "0.7"
                }
            }
        }""".trimIndent()
        val config = Config(json) {
            throw it
        }
        val longZones: List<MappingZone> = config.controlTuning.forwardLongZones
        Assert.assertEquals(4, longZones.size)

        Assert.assertEquals(0f, longZones[0].src.x)
        Assert.assertEquals(0.3f, longZones[0].src.y)
        Assert.assertEquals(0.01f, longZones[0].dst.x)
        Assert.assertEquals(0.21f, longZones[0].dst.y)

        Assert.assertEquals(0.3f, longZones[1].src.x)
        Assert.assertEquals(0.7f, longZones[1].src.y)
        Assert.assertEquals(0.21f, longZones[1].dst.x)
        Assert.assertEquals(0.4f, longZones[1].dst.y)

    }

    @Test
    fun testControlServer_ValidJson() {
        val json = "{\"control_server\": {\"address\": \"192.168.0.1\", \"port\": 8080}}"
        val config = Config(json)
        val server = config.controlServer
        Assert.assertNotNull(server)
        Assert.assertEquals("192.168.0.1", server!!.address)
        Assert.assertEquals(8080, server.port.toLong())
    }

    @Test
    fun testControlOffsets_DefaultValues() {
        val json = "{}"
        val config = Config(json)
        val offsets = config.controlOffsets
        Assert.assertEquals(0.0, offsets.pitch.toDouble(), 0.001)
        Assert.assertEquals(0.0, offsets.yaw.toDouble(), 0.001)
        Assert.assertEquals(0.0, offsets.steer.toDouble(), 0.001)
        Assert.assertEquals(0.0, offsets.long.toDouble(), 0.001)
    }

    @Test
    fun testControlTuning_EmptyJson() {
        val json = "{}"
        val config = Config(json)
        val tuning = config.controlTuning
        Assert.assertNull(tuning.pitchFactor)
        Assert.assertNull(tuning.pitchZone)
    }
}