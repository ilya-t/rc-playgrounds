package com.rc.playgrounds.config

import android.graphics.PointF
import com.rc.playgrounds.config.model.ControlOffsets
import com.rc.playgrounds.config.model.ControlTuning
import com.rc.playgrounds.config.model.MappingZone
import com.rc.playgrounds.config.model.NetworkTarget
import com.rc.playgrounds.config.stream.QualityProfile
import com.rc.playgrounds.config.stream.StreamConfig
import org.intellij.lang.annotations.Language
import org.json.JSONObject

data class Config(
    val stream: StreamConfig,
    val controlServer: NetworkTarget?,
    val streamTarget: NetworkTarget?,
    val controlOffsets: ControlOffsets,
    val controlTuning: ControlTuning,
) {

    fun writeToJson(): String {
        TODO("not implemented yet")
    }
    companion object {
        operator fun invoke(
            rawJson: String,
            errorCollector: (e: Throwable) -> Unit = { it.printStackTrace() },
        ): Config {
            val json = runCatching {
                JSONObject(rawJson)
            }
                .onFailure(errorCollector)
                .getOrElse { JSONObject() }

            return Config(
                stream = runCatching {
                    val stream = json.getJSONObject("stream")
                    val qualityProfiles = stream.optJSONArray("quality_profiles")?.let { array ->
                        List(array.length()) { index ->
                            val profile = array.getJSONObject(index)
                            QualityProfile(
                                width = profile.getInt("width"),
                                height = profile.getInt("height"),
                                bitrate = profile.getInt("bitrate"),
                                framerate = profile.getInt("framerate")
                            )
                        }
                    } ?: QualityProfile.DEFAULT_PROFILES

                    StreamConfig(
                        qualityProfiles = qualityProfiles,
                        remoteCmd = stream.optString("remote_cmd", ""),
                        localCmd = stream.optString("local_cmd", ""),
                    )
                }
                    .onFailure(errorCollector)
                    .getOrElse {
                        StreamConfig(
                            qualityProfiles = QualityProfile.DEFAULT_PROFILES,
                            remoteCmd = "",
                            localCmd = "",
                        )
                    },

                controlServer = runCatching {
                    val t = json.getJSONObject("control_server")
                    NetworkTarget(
                        address = t.getString("address"),
                        port = t.getInt("port")
                    )
                }
                    .onFailure(errorCollector)
                    .getOrNull(),

                streamTarget = runCatching {
                    val t = json.getJSONObject("stream_target")
                    NetworkTarget(
                        address = t.getString("address"),
                        port = t.getInt("port")
                    )

                }
                    .onFailure(errorCollector)
                    .getOrNull(),

                controlOffsets = runCatching {
                    val t = json.getJSONObject("control_offsets")
                    ControlOffsets(
                        pitch = t.getDouble("pitch").toFloat(),
                        yaw = t.getDouble("yaw").toFloat(),
                        steer = t.getDouble("steer").toFloat(),
                        long = t.getDouble("long").toFloat(),
                    )
                }.onFailure {
                    errorCollector(it)
                }.getOrElse {
                    ControlOffsets(
                        pitch = 0f,
                        yaw = 0f,
                        steer = 0f,
                        long = 0f,
                    )
                },

                controlTuning = runCatching {
                    val t = json.getJSONObject("control_tuning")
                    ControlTuning(
                        pitchFactor = t.optDouble("pitch_factor").toFloat(),
                        pitchZone = parseZone(t.optString("pitch_zone")),
                        yawFactor = t.optDouble("yaw_factor").toFloat(),
                        yawZone = parseZone(t.optString("yaw_zone")),
                        steerFactor = t.optDouble("steer_factor").toFloat(),
                        steerZone = parseZone(t.optString("steer_zone")),
                        forwardLongZones = parseZones(t.optJSONObject("forward_long_zones")),
                        backwardLongZones = parseZones(t.optJSONObject("backward_long_zones")),
                    )
                }
                    .onFailure { errorCollector.invoke(it) }
                    .getOrElse {
                        ControlTuning(
                            pitchFactor = null,
                            pitchZone = null,
                            yawFactor = null,
                            yawZone = null,
                            steerFactor = null,
                            steerZone = null,
                            forwardLongZones = emptyList(),
                            backwardLongZones = emptyList(),
                        )
                    },
            )
        }
    }
}

private fun parseZone(zone: String): PointF? {
    val minAndMax = zone.split("..")
    if (minAndMax.size != 2) {
        return null
    }

    val rawPoint = PointF(
        minAndMax[0].toFloat(),
        minAndMax[1].toFloat(),
    )

    if (rawPoint.x >= rawPoint.y) {
        return null
    }

    return rawPoint
}

private fun parseZones(zones: JSONObject?): List<MappingZone> {
    if (zones == null) {
        return emptyList()
    }
    val unsortedMapping = mutableListOf<PointF>()
    zones.keys().forEach { key ->
        val value = zones.getString(key)
        unsortedMapping.add(PointF(key.toFloat(), value.toFloat()))
    }

    val results = mutableListOf<MappingZone>()
    var srcZone = PointF(Float.NaN, Float.NaN)
    var dstZone = PointF(Float.NaN, Float.NaN)

    val mapping = unsortedMapping.sortedBy { it.x }
    mapping.forEach {
        if (results.isEmpty()) {
            results.add(MappingZone(
                src = PointF(it.x, Float.NaN),
                dst = PointF(it.y, Float.NaN),
            ))
            return@forEach
        }

        val lastResult = results.last()

        if (lastResult.src.y.isNaN()) {
            lastResult.src.y = it.x
            lastResult.dst.y = it.y
            return@forEach
        }

        results.add(MappingZone(
            src = PointF(lastResult.src.y, it.x),
            dst = PointF(lastResult.dst.y, it.y),
        ))
    }

    return results
}


@Language("Json")
internal const val DEFAULT_CONFIG = """
{
  "stream": {
    "local_cmd": "udpsrc port=12345 caps=\"application/x-rtp, media=video, encoding-name=H264, payload=96\" ! rtph264depay ! h264parse ! decodebin ! videoconvert ! autovideosink",
    "remote_cmd": "raspivid -pf baseline -fl -g 1 -w @{width} -h @{height} --bitrate @{bitrate} --nopreview -fps @{framerate}/1 -t 0 -o - | gst-launch-1.0 fdsrc ! h264parse ! rtph264pay ! udpsink host=@{stream_target} port=@{stream_target_port}",
    "quality_profiles": [
        {
            "width": 320,
            "height": 240,
            "framerate": 30,
            "bitrate": 800000
        },
        {
            "width": 640,
            "height": 480,
            "framerate": 30,
            "bitrate": 1600000
        },
        {
            "width": 1024,
            "height": 778,
            "framerate": 30,
            "bitrate": 3000000
        },
        {
            "width": 1280,
            "height": 720,
            "framerate": 30,
            "bitrate": 4200000
        },
        {
            "width": 1920,
            "height": 1080,
            "framerate": 30,
            "bitrate": 8000000
        }
    ]
  },
  "stream_target": {
    "address": "192.168.2.5",
    "port": 12345
  },  
  "control_server": {
    "address": "192.168.2.2",
    "port": 12346
  },
  "control_offsets": {
    "pitch": 0.0,
    "yaw": 0.0,
    "steer": -0.33,
    "long": 0.18
  },
  "control_tuning": {
    "pitch_factor": 1.0,
    "pitch_zone": "0..0.5",
    "yaw_factor": 1.0,
    "yaw_zone": "0..0.5",
    "steer_factor": 1.0,
    "steer_zone": "0..0.7",
    "forward_long_zones": {
        "0.0": "0.01",
        "0.5": "0.2",
        "1.0": "0.7"
    },
    "backward_long_zones": {
        "0.0": "0.01",
        "1.0": "0.2"
    }
  }
}    
"""

