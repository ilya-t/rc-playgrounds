package com.rc.playgrounds.remote.stream

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class StreamQualityProvider() {
    private val _quality = MutableStateFlow(StreamParameters.LOW)
    val currentQuality: Flow<StreamParameters> = _quality
}