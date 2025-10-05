package com.rc.playgrounds.config

import com.rc.playgrounds.config.model.ControlOffsets
import com.rc.playgrounds.config.model.ControlTuning
import com.rc.playgrounds.config.model.NetworkTarget
import com.rc.playgrounds.config.stream.QualityProfile
import com.rc.playgrounds.config.stream.StreamConfig
import com.rc.playgrounds.presentation.quickconfig.EnvironmentOverrides
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.intellij.lang.annotations.Language

@Serializable
data class Config(
    @SerialName("environment_variables")
    private val rawEnv: Map<String, String> = emptyMap(),
    @SerialName("environment_variables_overrides")
    val envOverrides: List<EnvironmentOverrides> = emptyList(),
    @SerialName("stream")
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
    @SerialName("control_profiles")
    val controlProfiles: List<ControlTuning> = listOf(
        ControlTuning(
            pitchFactor = "0",
            rawPitchZone = null,
            yawFactor = "0",
            rawYawZone = null,
            rawSteerZone = null,
            rawForwardLongZones = emptyMap(),
            rawBackwardLongZones = emptyMap(),
            wheel = null,
        )
    ),
) {
    val env: Map<String, String> = buildEnv(rawEnv, envOverrides)

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
                        rawEnv = emptyMap(),
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
                        controlProfiles = listOf(
                            ControlTuning(
                                pitchFactor = "0",
                                rawPitchZone = null,
                                yawFactor = "0",
                                rawYawZone = null,
                                rawSteerZone = null,
                                rawForwardLongZones = emptyMap(),
                                rawBackwardLongZones = emptyMap(),
                                wheel = null,
                            )
                        )
                    )
                }
        }
    }
}

private fun buildEnv(rawEnv: Map<String, String>,
                     overrides: List<EnvironmentOverrides>
): Map<String, String> {
    val env = rawEnv.toMutableMap()
    overrides.forEach {
        val lastActiveIndex = it.lastActiveIndex ?: return@forEach
        if (lastActiveIndex < 0) {
            return@forEach
        }

        val overrideProfiles = it.profiles.subList(
            fromIndex = 0,
            toIndex = lastActiveIndex.coerceIn(0, it.profiles.lastIndex) + 1
        )
        overrideProfiles.forEach { profile ->
            env.putAll(profile.env)
        }
    }

    return env
}


@Language("Json")
internal const val DEFAULT_CONFIG = """
{
    "environment_variables": {
        "fpv_car_server": "192.168.4.1",
        "fpv_car_server_port": "12346",
        "mobile_client_addr": "192.168.4.13",
        "mobile_client_port": "12345"
    },
    "environment_variables_overrides": [
        {
            "name": "stream quality",
            "last_active_index": 1,
            "override_profiles": [
                {
                    "name": "350x200",
                    "environment_variables": {
                        "width": "350",
                        "height": "200",
                        "framerate": "30",
                        "bitrate": "6000"
                    }
                },
                {
                    "name": "800x600 (0.06mb)",
                    "environment_variables": {
                        "width": "800",
                        "height": "600",
                        "bitrate": "6000"
                    }
                },
                {
                    "name": "800x600 (0.12mb)",
                    "environment_variables": {
                        "bitrate": "12000"
                    }
                },
                {
                    "name": "1024x768 (1mb)",
                    "environment_variables": {
                        "width": "1024",
                        "height": "768",
                        "bitrate": "1000000"
                    }
                },
                {
                    "name": "1024x778 (3mb)",
                    "environment_variables": {
                        "width": "1024",
                        "height": "778",
                        "framerate": "30",
                        "bitrate": "3000000"
                    }
                },
                {
                    "name": "1280x720 (4.2mb)",
                    "environment_variables": {
                        "width": "1280",
                        "height": "720",
                        "framerate": "30",
                        "bitrate": "4200000"
                    }
                },
                {
                    "name": "1920x1080 (8mb)",
                    "environment_variables": {
                        "width": "1920",
                        "height": "1080",
                        "framerate": "30",
                        "bitrate": "8000000"
                    }
                }
            ]
        },
        {
            "name": "control",
            "last_active_index": 0,
            "override_profiles": [
                {
                    "name": "balanced",
                    "environment_variables": {
                        "pitch_factor": "1",
                        "pitch_zone": "0..0.5",
                        "yaw_factor": "1.0",
                        "yaw_zone": "0..0.7",
                        "steer_zone": "-0.47..0.5",
                        "steer_mode": "exponent",
                        "steer_exponent_factor": "4.4"
                    }
                }
            ]
        },
        {
            "name": "network",
            "last_active_index": 0,
            "override_profiles": [
                {
                    "name": "mobile",
                    "environment_variables": {
                        "fpv_car_server": "192.168.2.2",
                        "mobile_client_addr": "192.168.2.4"
                    }
                },
                {
                    "name": "fpv_access_point",
                    "environment_variables": {
                        "fpv_car_server": "192.168.4.1",
                        "mobile_client_addr": "192.168.4.13"
                    }
                }
            ]
        }
    ],
    "stream": {
        "local_cmd": "udpsrc port=@{mobile_client_port} caps=\"application/x-rtp, media=video, encoding-name=H264, payload=96\" ! rtph264depay ! h264parse ! decodebin ! videoconvert ! autovideosink",
        "remote_cmd": "raspivid -pf baseline -fl -g 1 -w @{width} -h @{height} --bitrate @{bitrate} --nopreview -fps @{framerate}/1 -t 0 -o - | gst-launch-1.0 fdsrc ! h264parse ! rtph264pay ! udpsink host=@{mobile_client_addr} port=@{mobile_client_port}"
    },
    "stream_target": {
        "address": "@{mobile_client_addr}",
        "port": "@{mobile_client_port}"
    },
    "control_server": {
        "address": "@{fpv_car_server}",
        "port": "@{fpv_car_server_port}"
    },
    "control_offsets": {
        "pitch": 0.0,
        "yaw": 0.0,
        "steer": 0.08,
        "long": 0.18
    },
    "_control_profiles_comment_": "control_profiles switched via gamepad bumpers. Each profile overrides props of last one so on.",
    "control_profiles": [
        {
            "name": "default",
            "pitch_factor": "@{pitch_factor}",
            "pitch_zone": "@{pitch_zone}",
            "yaw_factor": "@{yaw_factor}",
            "yaw_zone": "@{yaw_zone}",
            "steer_zone": "@{steer_zone}",
            "_steer_mode_comment_": "available modes 'steer_limit_at_trigger', 'wheel' and 'exponent'",
            "steer_mode": "@{steer_mode}",
            "steer_exponent_factor": "@{steer_exponent_factor}",
            "steer_limit_at_trigger": {
                "0.0": "1",
                "0.01": "0.2",
                "0.7": "0.3",
                "1.0": "0.5"
            },
            "forward_long_zones": {
                "0.28": "0.01",
                "0.5": "0.2",
                "0.9": "0.5",
                "1.0": "0.6"
            },
            "backward_long_zones": {
                "0.0": "0.01",
                "0.9": "0.5"
            },
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
        },
        {
            "name": "crawling",
            "forward_long_zones": {
                "0.28": "0.01",
                "0.5": "0.1",
                "0.9": "0.2",
                "1.0": "0.3"
            },
            "backward_long_zones": {
                "0.0": "0.01",
                "0.9": "0.4"
            }
        },
        {
            "name": "maximum long",
            "forward_long_zones": {
                "0.28": "0.01",
                "0.5": "0.1",
                "0.9": "0.5",
                "1.0": "1.0"
            },
            "backward_long_zones": {
                "0.0": "0.01",
                "0.5": "0.4",
                "1.0": "1.0"
            }
        }
    ]
}
"""

