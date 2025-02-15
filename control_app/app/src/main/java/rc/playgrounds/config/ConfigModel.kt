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
    private val _configFlow = MutableStateFlow<Config>(Config(DEFAULT_CONFIG))
    val configFlow: StateFlow<Config> = _configFlow

    @OptIn(ExperimentalCoroutinesApi::class)
    private val scope = scope + Dispatchers.IO.limitedParallelism(1)

    fun updateConfig(json: String) {
        scope.launch {
            if (_configFlow.value.rawJson == json) {
                return@launch
            }

            _configFlow.value =  Config(json)
            storage.writeString(STORAGE_KEY, json)
        }
    }

    init {
        scope.launch {
            val config = storage.readString(STORAGE_KEY).takeIf { it?.isNotEmpty() == true } ?: DEFAULT_CONFIG
            _configFlow.value = Config(config)
        }
    }
}

private const val STORAGE_KEY = "config"
@Language("Json")
private const val DEFAULT_CONFIG = """
{
  "stream": {
    "url": "udp://@:12345"
  },
  "telemetry": {
    "address": "192.168.1.2",
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
    "yaw_factor": 1.0,
    "steer_factor": 1.0,
    "long_factor": 1.0
  }
}    
"""