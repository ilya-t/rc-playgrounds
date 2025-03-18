package com.rc.playgrounds.control.gamepad

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class GamepadEventStream {
    private val _events = MutableStateFlow(
        GamepadEvent.INITIAL
    )

    val events: StateFlow<GamepadEvent> = _events

    fun emit(event: GamepadEvent) {
        _events.value = event
    }
}