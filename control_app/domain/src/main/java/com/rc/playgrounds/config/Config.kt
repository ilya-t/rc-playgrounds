package com.rc.playgrounds.config

import com.rc.playgrounds.config.model.ControlOffsets
import com.rc.playgrounds.config.model.ControlTuning
import com.rc.playgrounds.config.model.NetworkTarget
import com.rc.playgrounds.config.stream.QualityProfile
import com.rc.playgrounds.config.stream.StreamConfig
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.intellij.lang.annotations.Language

@Serializable
data class Config(
    val stream: StreamConfig,
    @SerialName("control_server")
    val controlServer: NetworkTarget?,
    @SerialName("stream_target")
    val streamTarget: NetworkTarget?,
    @SerialName("control_offsets")
    val controlOffsets: ControlOffsets = ControlOffsets(
        pitch = 0f,
        yaw = 0f,
        steer = 0f,
        long = 0f,
    ),
    @SerialName("control_tuning")
    val controlTuning: ControlTuning = ControlTuning(
        pitchFactor = 0f,
        rawPitchZone = null,
        yawFactor = 0f,
        rawYawZone = null,
        rawSteerZone = null,
        rawForwardLongZones = emptyMap(),
        rawBackwardLongZones = emptyMap(),
        wheel = null,
    ),
) {
    fun writeToJson(): String {
        return jsonParser.encodeToString(Config.serializer(), this)
    }

    companion object {
        private val jsonParser = Json {
            ignoreUnknownKeys = true
            prettyPrint = true
            isLenient = true
        }

        operator fun invoke(
            rawJson: String,
            errorCollector: (Throwable) -> Unit = { it.printStackTrace() },
        ): Config {
            return runCatching {
                jsonParser.decodeFromString<Config>(rawJson)
            }.onFailure(errorCollector)
                .getOrElse {
                    Config(
                        stream = StreamConfig(
                            qualityProfiles = QualityProfile.DEFAULT_PROFILES,
                            remoteCmd = "",
                            localCmd = ""
                        ),
                        controlServer = null,
                        streamTarget = null,
                        controlOffsets = ControlOffsets(
                            pitch = 0f,
                            yaw = 0f,
                            steer = 0f,
                            long = 0f,
                        ),
                        controlTuning = ControlTuning(
                            pitchFactor = 0f,
                            rawPitchZone = null,
                            yawFactor = 0f,
                            rawYawZone = null,
                            rawSteerZone = null,
                            rawForwardLongZones = emptyMap(),
                            rawBackwardLongZones = emptyMap(),
                            wheel = null,
                        )
                    )
                }
        }
    }
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
    "steer": 0.08,
    "long": 0.18
  },
  "control_tuning": {
    "pitch_factor": 1.0,
    "pitch_zone": "0..0.5",
    "yaw_factor": 1.0,
    "yaw_zone": "0..0.5",
    "steer_zone": "-0.7..0.7",
    "steer_limit_at_trigger": {
        "0.0": "1.0",
        "0.5": "0.7",
        "1.0": "0.3"
    },
    "forward_long_zones": {
        "0.0": "0.01",
        "0.5": "0.2",
        "1.0": "0.7"
    },
    "backward_long_zones": {
        "0.0": "0.01",
        "1.0": "0.2"
    },
    "_comment_steer_mode_": "available modes 'steer_limit_at_trigger', 'wheel' and 'exponent'",
    "steer_mode": "wheel",
    "steer_exponent_factor": 2.0,
    "wheel": {
      "_comment_": "All values are optional. See: WheelEmulator.kt",
      "max_angle_deg": 28.0,
      "max_turn_rate_deg_per_sec": 420.0,
      "center_return_rate_deg_per_sec": 140.0,
      "deadzone": 0.06,
      "curve_blend": 0.55,
      "ema_cutoff_hz": 10.0,
      "center_stick_threshold": 0.02,
      "damping": 0.9
    }
  }
}
"""

