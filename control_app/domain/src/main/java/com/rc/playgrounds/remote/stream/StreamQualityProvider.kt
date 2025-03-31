package com.rc.playgrounds.remote.stream

import com.rc.playgrounds.config.ActiveConfigProvider
import com.rc.playgrounds.config.stream.QualityProfile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class StreamQualityProvider(
    private val activeConfigProvider: ActiveConfigProvider,
    private val scope: CoroutineScope,
) {
    private val _quality = MutableStateFlow(QualityProfile.DEFAULT_PROFILES.first())

    val currentQuality: Flow<QualityProfile> = _quality

    init {
        scope.launch {
            activeConfigProvider.configFlow.map { it.stream }.collect { stream ->
                invalidate(stream.defaultQualityProfile ?: 0, stream.qualityProfiles)
            }
        }
    }

    fun nextQuality() {
        moveQualityProfileIndexBy(1)
    }

    fun prevQuality() {
        moveQualityProfileIndexBy(-1)
    }

    private fun moveQualityProfileIndexBy(i: Int) {
        activeConfigProvider.update { config ->
            var index = (config.stream.defaultQualityProfile ?: 0) + i
            val current = config.stream.qualityProfiles.getOrNull(index)

            if (index < 0) {
                index = 0
            }

            if (current == null) {
                index = config.stream.qualityProfiles.lastIndex
            }

            config.copy(
                stream = config.stream.copy(
                    defaultQualityProfile = index,
                )
            )
        }
    }

    private fun invalidate(index: Int, qualityProfiles: List<QualityProfile>) {
        val current = qualityProfiles.getOrNull(index) ?: return
        _quality.value = current
    }
}
