package com.rc.playgrounds.remote.stream

import com.rc.playgrounds.config.ActiveConfigProvider
import com.rc.playgrounds.config.stream.QualityProfile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class StreamQualityProvider(
    private val activeConfigProvider: ActiveConfigProvider,
    private val scope: CoroutineScope,
) {
    private val _quality = MutableStateFlow(QualityProfile.DEFAULT_PROFILES.first())
    private val profileIndex = MutableStateFlow<Int>(0)

    val currentQuality: Flow<QualityProfile> = _quality

    init {
        scope.launch {
            combine(
                activeConfigProvider.configFlow.map { it.stream.qualityProfiles },
                profileIndex,
            ) { a, b -> a to b }
            .collect { (profiles, index) ->
                invalidate(index, profiles)
            }
        }
    }

    fun nextQuality() {
        profileIndex.value += 1
    }


    fun prevQuality() {
        profileIndex.value -= 1
    }

    private fun invalidate(index: Int, qualityProfiles: List<QualityProfile>) {
        val current = qualityProfiles.getOrNull(index)

        if (current != null) {
            _quality.value = current
            return
        }

        if (index < 0) {
            profileIndex.value = 0
            return
        }

        profileIndex.value = qualityProfiles.lastIndex
    }
}