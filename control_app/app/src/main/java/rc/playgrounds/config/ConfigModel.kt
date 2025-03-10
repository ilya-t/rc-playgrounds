package rc.playgrounds.config

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import org.intellij.lang.annotations.Language
import rc.playgrounds.storage.PersistentStorage

class ConfigModel(
    scope: CoroutineScope,
    private val storage: PersistentStorage,
) {
    private val _configFlow = MutableStateFlow<com.rc.playgrounds.config.Config>(
        com.rc.playgrounds.config.Config(
            DEFAULT_CONFIG
        )
    )
    val configFlow: StateFlow<com.rc.playgrounds.config.Config> = _configFlow

    @OptIn(ExperimentalCoroutinesApi::class)
    private val scope = scope + Dispatchers.IO.limitedParallelism(1)

    fun updateConfig(json: String) {
        scope.launch {
            if (_configFlow.value.rawJson == json) {
                return@launch
            }

            _configFlow.value = com.rc.playgrounds.config.Config(json)
            storage.writeString(STORAGE_KEY, json)
        }
    }

    init {
        scope.launch {
            val config = storage.readString(STORAGE_KEY).takeIf { it?.isNotEmpty() == true } ?: DEFAULT_CONFIG
            _configFlow.value = com.rc.playgrounds.config.Config(config)
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
private const val REMOTE_CMD = "raspivid -pf baseline -awb cloud -fl -g 1 -w 320 -h 240 --nopreview -fps 30/1 -t 0 -o - | gst-launch-1.0 fdsrc ! h264parse ! rtph264pay ! udpsink host=$SERVER_STREAM_TARGET port=12345"

@Language("Json")
private const val DEFAULT_CONFIG = """
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
    "steer": 0.0,
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
    "long_zones": {
        "0.0": "0.01",
        "0.5": "0.2",
        "1.0": "0.7"
    }
  }
}    
"""