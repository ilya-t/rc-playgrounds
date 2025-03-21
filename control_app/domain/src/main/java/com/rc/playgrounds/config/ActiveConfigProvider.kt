package com.rc.playgrounds.config

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import org.intellij.lang.annotations.Language

class ActiveConfigProvider(
    scope: CoroutineScope,
    private val configRepository: ConfigRepository
) {
    val configFlow: Flow<Config> = configRepository.activeVersion.map { Config(it.rawConfig) }

    @OptIn(ExperimentalCoroutinesApi::class)
    private val scope = scope + Dispatchers.IO.limitedParallelism(1)

    fun updateConfig(json: String) {
        scope.launch {
            val version = configRepository.activeVersion.value.version
            configRepository.storeConfig(
                ConfigVersion(
                    version = version,
                    rawConfig = json,
                )
            )
            configRepository.switchActive(version)
        }
    }
}

private const val STORAGE_KEY = "config"

// m1+pixel+fake
//private const val SERVER_ADDR = "192.168.1.182"
//private const val SERVER_STREAM_TARGET = "192.168.1.181"
//private const val REMOTE_CMD = "python3 fake_video_stream.py | gst-launch-1.0 --verbose fdsrc ! h264parse ! rtph264pay ! udpsink host=$SERVER_STREAM_TARGET port=12345"

// defaults setup
private const val SERVER_ADDR = "192.168.2.2"
private const val SERVER_STREAM_TARGET = "192.168.2.5"
private const val REMOTE_CMD = "raspivid -pf baseline -fl -g 1 -w 320 -h 240 --nopreview -fps 30/1 -t 0 -o - | gst-launch-1.0 fdsrc ! h264parse ! rtph264pay ! udpsink host=$SERVER_STREAM_TARGET port=12345"

// receive mjpeg
// "local_cmd": "udpsrc port=12345 ! jpegparse ! jpegdec ! autovideosink"
//
@Language("Json")
internal const val DEFAULT_CONFIG = """
{
  "stream": {
    "remote_cmd": "$REMOTE_CMD",
    "local_cmd": "udpsrc port=12345 caps=\"application/x-rtp, media=video, encoding-name=H264, payload=96\" ! rtph264depay ! h264parse ! decodebin ! videoconvert ! autovideosink"
  },
  "control_server": {
    "address": "$SERVER_ADDR",
    "port": 12346
  },
  "control_offsets": {
    "pitch": 0.0,
    "yaw": 0.0,
    "steer": -0.33,
    "long": 0.0
  },
  "control_tuning": {
    "pitch_factor": 1.0,
    "pitch_zone": "0..0.5",
    "yaw_factor": 1.0,
    "yaw_zone": "0..0.5",
    "steer_factor": 1.0,
    "steer_zone": "0..0.7",
    "long_factor": 0.5,
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