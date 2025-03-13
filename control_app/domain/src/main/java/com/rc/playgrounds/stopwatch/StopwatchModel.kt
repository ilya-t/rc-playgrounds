package com.rc.playgrounds.stopwatch

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Locale

class StopwatchModel(private val scope: CoroutineScope) {
    fun toggle() {
        if (_state.value == null) {
            start()
        } else {
            stop()
        }
    }

    private fun stop() {
        job?.cancel()
        job = null
        _state.value = null
    }

    private fun start() {
        job?.cancel()
        job = scope.launch {
            while (isActive) {
                _state.emit(generateTime())
            }
        }

    }

    private fun generateTime(): String {
        val calendar = Calendar.getInstance()
        val second = calendar.get(Calendar.SECOND)
        val millisecond = calendar.get(Calendar.MILLISECOND)
        return String.format(Locale.US, "%02d:%03d", second, millisecond)
    }


    private var job: Job? = null
    private val _state = MutableStateFlow<String?>(null)
    val state: Flow<String?> = _state

}
