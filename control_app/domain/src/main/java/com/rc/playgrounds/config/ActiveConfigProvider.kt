package com.rc.playgrounds.config

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ActiveConfigProvider(
    private val configRepository: ConfigRepository
) {
    val configFlow: Flow<Config> = configRepository.activeVersion.map { Config(it.rawConfig) }
}
