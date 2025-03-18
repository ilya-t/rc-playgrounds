package com.rc.playgrounds.status

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class StreamerEvents {
    private val _events = MutableStateFlow<Result<String>>(Result.success(""))
    internal val events: Flow<Result<String>> = _events

    fun emit(error: Exception) {
        _events.value = Result.failure(error)
    }

    fun emit(event: String) {
        _events.value = Result.success(event)
    }
}