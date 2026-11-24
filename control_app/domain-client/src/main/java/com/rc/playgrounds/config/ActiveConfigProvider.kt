package com.rc.playgrounds.config

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class ActiveConfigProvider(
    private val configRepository: ConfigRepository,
    private val scope: CoroutineScope,
) {
    val configFlow: Flow<Config> = configRepository.activeVersion.map { Config(it.rawConfig) }

    fun update(transform: (Config) -> Config) {
        scope.launch {
            val current: Config = configFlow.first()
            val new: Config = transform(current)
            configRepository.storeConfig(
                ConfigVersion(
                    version = configRepository.activeVersion.value.version,
                    rawConfig = new.writeToJson(),
                )
            )
        }
    }
}
