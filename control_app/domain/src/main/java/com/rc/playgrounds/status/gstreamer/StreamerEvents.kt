package com.rc.playgrounds.status.gstreamer

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

class StreamerEvents(
    private val scope: CoroutineScope,
) {
    private val _events = Channel<Event>(capacity = 1000)
    internal val events: Flow<Event> = _events.receiveAsFlow()

    fun emit(error: Exception) {
        scope.launch {
            _events.send(Event.Error(error))
        }
    }

    fun emit(event: String) {
        scope.launch {
            _events.send(Event.Message(event))
        }
    }
}