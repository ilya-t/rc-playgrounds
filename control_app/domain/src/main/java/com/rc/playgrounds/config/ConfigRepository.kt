package com.rc.playgrounds.config

import com.rc.playgrounds.storage.PersistentStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import org.json.JSONArray


class ConfigRepository(
    private val persistentStorage: PersistentStorage,
    scope: CoroutineScope,
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    private val scope = scope + Dispatchers.IO.limitedParallelism(1)
    private val _allVersions = MutableStateFlow(loadAvailableVersions(persistentStorage))
    val allVersions: StateFlow<List<String>> = _allVersions

    private val _activeVersion = MutableStateFlow(
        run {
            val version = loadActiveVersionName(persistentStorage)
                ?: _allVersions.value.lastOrNull() ?: pickNextVersion(emptyList())
            ConfigVersion(
                version = version,
                rawConfig = readVersionConfig(version) ?: DEFAULT_CONFIG
            )
        }
    )
    val activeVersion: StateFlow<ConfigVersion> = _activeVersion

    fun storeConfig(newVersion: ConfigVersion): Deferred<Result<Unit>> {
        return scope.async {
            isValid(newVersion).onFailure {
                return@async Result.failure(it)
            }

            writeVersionConfig(newVersion.version, newVersion.rawConfig)

            val active = _activeVersion.value
            if (newVersion.version != active.version) {
                return@async Result.success(Unit)
            }

            if (newVersion.rawConfig == active.rawConfig) {
                return@async Result.success(Unit)
            }

            _activeVersion.value = newVersion
            Result.success(Unit)
        }
    }

    private fun isValid(c: ConfigVersion): Result<Unit> {
        val errors = mutableListOf<Throwable>()
        Config(c.rawConfig) { t: Throwable ->
            errors.add(t)
        }

        return if (errors.isEmpty()) {
            Result.success(Unit)
        } else {
            Result.failure(errors.first())
        }
    }

    fun switchActive(version: String) {
        scope.launch {
            persistentStorage.writeString(ACTIVE_CONFIG_NAME, version)

            if (_activeVersion.value.version == version) {
                return@launch
            }
            val config: String = readVersionConfig(version) ?: return@launch
            _activeVersion.value = ConfigVersion(version, rawConfig = config)
        }
    }

    private fun readVersionConfig(version: String): String? {
        return persistentStorage.readString("$CONFIG_PREFIX$version")
    }

    private suspend fun writeVersionConfig(version: String, config: String) {
        val prefixedVersion = "$CONFIG_PREFIX$version"
        persistentStorage.writeString(prefixedVersion, config)
        val configVersions = _allVersions.value.toMutableList()
        if (!configVersions.contains(version)) {
            configVersions.add(version)
            persistentStorage.writeString(CONFIGS, JSONArray(configVersions).toString())
            _allVersions.value = configVersions.toList()
        }
    }
}

private fun loadAvailableVersions(persistentStorage: PersistentStorage): List<String> {
    val raw = persistentStorage.readString(CONFIGS) ?: return emptyList()
    val jsonArray = JSONArray(raw)
    val configList = mutableListOf<String>()
    for (i in 0 until jsonArray.length()) {
        configList.add(jsonArray.getString(i))
    }
    return configList
}

internal fun pickNextVersion(allVersions: List<String>): String {
    var i = 1
    var candidate = "v.${allVersions.size + i}"

    while (allVersions.contains(candidate)) {
        i++
        candidate = "v.${allVersions.size + i}"
    }

    return candidate
}

private fun loadActiveVersionName(persistentStorage: PersistentStorage): String? {
    return persistentStorage.readString(ACTIVE_CONFIG_NAME)
}

private const val CONFIGS = "all_configs"
private const val ACTIVE_CONFIG_NAME = "active_config"
private const val CONFIG_PREFIX = "config_"