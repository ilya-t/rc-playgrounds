package com.rc.playgrounds.status.gstreamer

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class FrameDropStatus(
    private val streamerEvents: Flow<Event>,
    private val scope: CoroutineScope,
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    private val dispatcher = Dispatchers.Default.limitedParallelism(1)
    private val _state = MutableStateFlow<Int>(-1)
    private val qosEvents = mutableListOf<Event>()
    val frameDropsPerSecond: Flow<Int> = _state

    init {
        scope.launch(dispatcher) {
            streamerEvents
                .filter { it.isFrameDropEvent() }
                .collect {
                    qosEvents.add(it)
                    invalidate()
                }
        }

        scope.launch(dispatcher) {
            while (isActive) {
                delay(200L)
                invalidate()
            }
        }
    }

    private fun invalidate() {
        // Remove events not from current second
        val currentTime = System.currentTimeMillis()
        qosEvents.removeAll { event ->
            currentTime - event.time > 1000
        }

        // Count size of events
        val frameDrops = qosEvents.size

        // Emit to _state
        _state.value = frameDrops
    }
}

private fun Event.isFrameDropEvent(): Boolean {
    return this is Event.Message && this.message.contains("Dropping frame due to QoS.")
}
